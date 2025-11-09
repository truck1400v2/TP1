package com.chat.serveur;

import com.commun.evenement.Evenement;
import com.commun.evenement.GestionnaireEvenement;
import com.commun.net.Connexion;

/**
 * Cette classe représente un gestionnaire d'événement d'un serveur.
 * Lorsqu'un serveur reçoit un texte d'un client, il crée un événement à partir du texte reçu
 * et alerte ce gestionnaire qui réagit en gérant l'événement.
 */
public class GestionnaireEvenementServeur implements GestionnaireEvenement {
    private Serveur serveur;

    public GestionnaireEvenementServeur(Serveur serveur) {
        this.serveur = serveur;
    }

    @Override
    public void traiter(Evenement evenement) {
        Object source = evenement.getSource();
        Connexion cnx;
        String msg, typeEvenement, aliasExpediteur;
        ServeurChat serveur = (ServeurChat) this.serveur;

        if (source instanceof Connexion) {
            cnx = (Connexion) source;

            // --- Normaliser la commande en MAJUSCULES ---
            typeEvenement = evenement.getType();
            String typeUC = (typeEvenement == null) ? "" : typeEvenement.trim().toUpperCase();
            System.out.println("SERVEUR-Recu : " + typeUC + " " + evenement.getArgument());

            switch (typeUC) {

                case "EXIT": { // Ferme la connexion avec le client qui a envoyé "EXIT"
                    aliasExpediteur = cnx.getAlias();          // alias qui quitte
                    // Nettoyer invitations + salons et notifier les partenaires
                    serveur.nettoyerDeconnexion(aliasExpediteur);
                    cnx.envoyer("INFO bye :( "  + aliasExpediteur + "\n");
                    cnx.envoyer("END");
                    serveur.enlever(cnx);                      // <-- enlève des connectés (LIST ne l’affichera plus)
                    cnx.close();
                    break;
                }


                case "LIST": { // renvoyer tous sauf moi (affichés en MAJ)
                    aliasExpediteur = cnx.getAlias();
                    StringBuilder sb = new StringBuilder();
                    String all = serveur.list(); // "a:b:c:"
                    if (all != null && !all.isEmpty()) {
                        for (String a : all.split(":")) {
                            if (a == null || a.isEmpty()) continue;
                            if (a.equalsIgnoreCase(aliasExpediteur)) continue; // exclure moi
                            if (sb.length() > 0) sb.append(':');
                            sb.append(a.toUpperCase()); // homogénéiser
                        }
                    }
                    cnx.envoyer("LIST " + sb.toString());
                    break;
                }

                case "MSG": {
                    // Ne pas uppercaser le contenu du message, seulement la commande
                    aliasExpediteur = cnx.getAlias();
                    msg = evenement.getArgument(); // message tel que saisi
                    serveur.envoyerATousSauf(msg, aliasExpediteur);
                    break;
                }

                case "HIST": {
                    String hist = serveur.historique(); // "ligne1\nligne2\n..."
                    if (!hist.isEmpty()) {
                        cnx.envoyer("HIST " + hist);
                    } else {
                        cnx.envoyer("Historique est vide...");
                    }
                    break;
                }

                case "PRV": {
                    // Format attendu: PRV alias2 message...
                    aliasExpediteur = cnx.getAlias();           // alias1 (canonique côté serveur)
                    String moiUC = aliasExpediteur.toUpperCase();

                    String argRaw = evenement.getArgument();    // "alias2 message..."
                    if (argRaw == null || argRaw.trim().isEmpty()) {
                        cnx.envoyer("ERR PRV INVALIDE");
                        break;
                    }

                    argRaw = argRaw.trim();
                    int p = argRaw.indexOf(' ');
                    String alias2Saisi, texte;
                    if (p < 0) {
                        // pas de message fourni
                        alias2Saisi = argRaw;
                        texte = "";
                    } else {
                        alias2Saisi = argRaw.substring(0, p).trim();
                        texte       = argRaw.substring(p + 1).trim();
                    }

                    // gardes-fous
                    if (alias2Saisi.isEmpty() || alias2Saisi.equalsIgnoreCase(aliasExpediteur)) {
                        cnx.envoyer("ERR PRV INVALIDE");
                        break;
                    }
                    if (texte.isEmpty()) {
                        cnx.envoyer("ERR PRV MESSAGE VIDE");
                        break;
                    }

                    // alias2 doit être connecté
                    Connexion cible = serveur.trouverParAlias(alias2Saisi);
                    if (cible == null) {
                        cnx.envoyer("USER N'EXISTE PAS");
                        break;
                    }

                    // vérifier l'existence du salon privé (ordre insensible)
                    String alias2Canon   = cible.getAlias();
                    String alias2CanonUC = alias2Canon.toUpperCase();
                    if (!serveur.existeSalon(aliasExpediteur, alias2Canon)) {
                        cnx.envoyer("ERR PAS DE SALON PRIVÉ AVEC " + alias2CanonUC);
                        break;
                    }

                    // OK : envoyer le message privé
                    cnx.envoyer("PRV " + alias2CanonUC + " " + texte);
                    // - message côté destinataire: montre qui parle
                    cible.envoyer("PRV " + moiUC + " " + texte);
                    break;
                }

                case "QUIT": {
                    // Format: QUIT alias2  => quitter le salon privé avec alias2
                    aliasExpediteur = cnx.getAlias();                // moi
                    String moiUC = aliasExpediteur.toUpperCase();

                    String argRaw = evenement.getArgument();         // "alias2"
                    String alias2Saisi = (argRaw == null) ? "" : argRaw.trim();
                    String alias2SaisiUC = alias2Saisi.toUpperCase();

                    // gardes-fous
                    if (alias2Saisi.isEmpty() || alias2Saisi.equalsIgnoreCase(aliasExpediteur)) {
                        cnx.envoyer("ERR QUIT INVALIDE");
                        break;
                    }

                    // si alias2 est connecté, récupérer son alias canonique ; sinon garder tel quel
                    Connexion cible = serveur.trouverParAlias(alias2Saisi);
                    String alias2Canon   = (cible != null) ? cible.getAlias() : alias2Saisi;
                    String alias2CanonUC = alias2Canon.toUpperCase();

                    // vérifier l'existence du salon privé (ordre insensible)
                    if (!serveur.existeSalon(aliasExpediteur, alias2Canon)) {
                        cnx.envoyer("ERR PAS DE SALON PRIVÉ AVEC " + alias2CanonUC);
                        break;
                    }

                    // supprimer le salon privé
                    serveur.supprimerSalon(aliasExpediteur, alias2Canon);

                    // confirmer à l'émetteur
                    cnx.envoyer("INFO Vous avez quitté la salle avec " + alias2CanonUC);

                    // notifier l'autre participant s'il est connecté
                    if (cible != null) {
                        // Le client côté cible affichera: "<moi> a quitté le salon privé."
                        cible.envoyer("QUIT " + moiUC);
                    }
                    break;
                }

                case "INV": {
                    // aliasExpediteur veut la liste des invitations qu'il a reçues
                    aliasExpediteur = cnx.getAlias();
                    String liste = serveur.invitationsRecues(aliasExpediteur); // "A:B:C" ou "" si aucune
                    cnx.envoyer("INV " + liste);
                    break;
                }



                case "JOIN": {
                    aliasExpediteur = cnx.getAlias();      // alias1 (canonique)
                    msg = evenement.getArgument();         // "alias2" saisi
                    String alias2Saisi = (msg == null ? "" : msg.trim());

                    // pas d'auto-invitation / alias vide
                    if (alias2Saisi.isEmpty() || alias2Saisi.equalsIgnoreCase(aliasExpediteur)) {
                        cnx.envoyer("DECLINE " + alias2Saisi);
                        break;
                    }

                    // la cible doit exister (être connectée)
                    Connexion cible = serveur.trouverParAlias(alias2Saisi);
                    if (cible == null) {
                        cnx.envoyer("USER N'EXISTE PAS");
                        break; // on NE crée ni salon ni invitation
                    }

                    // alias canonique de la cible
                    String alias2Canon   = cible.getAlias();
                    String alias2CanonUC = alias2Canon.toUpperCase();
                    String moiUC         = aliasExpediteur.toUpperCase();

                    // 0) Déjà en salon privé ensemble ? => ne rien recréer
                    if (serveur.existeSalon(aliasExpediteur, alias2Canon)) {
                        cnx.envoyer("INFO Vous êtes déjà en chat privé avec " + alias2CanonUC);
                        break;
                    }

                    // 1) invitation inverse déjà présente ? -> accepter automatiquement
                    if (serveur.existeInvitation(alias2Canon, aliasExpediteur)) {
                        serveur.supprimerInvitation(alias2Canon, aliasExpediteur);
                        serveur.ajouterSalon(aliasExpediteur, alias2Canon); // ordre insensible
                        cnx.envoyer("JOINOK " + alias2CanonUC);
                        cible.envoyer("JOINOK " + moiUC);
                        break;
                    }

                    // 2) sinon, poser l'invitation (sans doublon) et notifier alias2
                    if (!serveur.existeInvitation(aliasExpediteur, alias2Canon)) {
                        serveur.ajouterInvitation(aliasExpediteur, alias2Canon);
                    }
                    cible.envoyer("JOIN " + moiUC);

                    // (facultatif) confirmation locale
                    cnx.envoyer("INFO Invitation a été envoyée à " + alias2CanonUC);
                    break;
                }


                case "JOINOK": {
                    // Autoriser JOINOK SEULEMENT si:
                    // - l'alias ciblé existe (connecté)
                    // - il y a une invitation aliasCible -> aliasExpediteur
                    aliasExpediteur = cnx.getAlias();          // moi (qui accepte)
                    String moiUC = aliasExpediteur.toUpperCase();

                    String argRaw = evenement.getArgument();   // alias saisi
                    String aliasCibleSaisi = (argRaw == null) ? "" : argRaw.trim();
                    String aliasCibleSaisiUC = aliasCibleSaisi.toUpperCase();




                    // invalide / auto / vide
                    if (aliasCibleSaisi.isEmpty() || aliasCibleSaisi.equalsIgnoreCase(aliasExpediteur)) {
                        cnx.envoyer("ERR JOINOK INVALIDE");
                        break;
                    }

                    // Résoudre vers l'alias EXACT (canonique), mais on affichera en MAJ
                    Connexion cible = serveur.trouverParAlias(aliasCibleSaisi);
                    if (cible == null) {
                        cnx.envoyer("USER N'EXISTE PAS");
                        break;
                    }
                    String aliasCible = cible.getAlias();
                    String aliasCibleUC = aliasCible.toUpperCase();

                    // Vérifier l'invitation (aliasCible -> aliasExpediteur)
                    if (!serveur.existeInvitation(aliasCible, aliasExpediteur)) {
                        // Si c'est MOI qui ai invité l'autre, expliquer l'état
                        if (serveur.existeInvitation(aliasExpediteur, aliasCible)) {
                            cnx.envoyer("ERR TU AS INVITÉ " + aliasCibleUC + " — ATTENDS SON JOINOK");
                        } else {
                            cnx.envoyer("ERR PAS D'INVITATION DE " + aliasCibleUC);
                        }
                        break;
                    }

                    // OK : retirer l'invitation et créer le salon
                    serveur.supprimerInvitation(aliasCible, aliasExpediteur);
                    serveur.ajouterSalon(aliasExpediteur, aliasCible);

                    // Notifier les deux (on envoie l'autre alias, en MAJ pour l'affichage)
                    cnx.envoyer("JOINOK " + aliasCibleUC);
                    cible.envoyer("JOINOK " + moiUC);
                    break;
                }

                case "DECLINE": {
                    aliasExpediteur = cnx.getAlias();          // alias1
                    String moiUC = aliasExpediteur.toUpperCase();

                    String argRaw = evenement.getArgument();   // "alias2"
                    String alias2Saisi = (argRaw == null) ? "" : argRaw.trim();
                    String alias2SaisiUC = alias2Saisi.toUpperCase();

                    // garde-fous
                    if (alias2Saisi.isEmpty() || alias2Saisi.equalsIgnoreCase(aliasExpediteur)) {
                        cnx.envoyer("ERR DECLINE INVALIDE");
                        break;
                    }



                    // 1) REFUSER une invitation REÇUE : (alias2 -> alias1)
                    if (serveur.existeInvitation(alias2Saisi, aliasExpediteur)) {
                        serveur.supprimerInvitation(alias2Saisi, aliasExpediteur);

                        // informer alias2 si connecté
                        Connexion cible = serveur.trouverParAlias(alias2Saisi);
                        if (cible != null) {
                            cible.envoyer("DECLINE " + moiUC);
                        }
                        break;
                    }

                    // 2) ANNULER une invitation ENVOYÉE : (alias1 -> alias2)
                    if (serveur.existeInvitation(aliasExpediteur, alias2Saisi)) {
                        serveur.supprimerInvitation(aliasExpediteur, alias2Saisi);

                        Connexion cible = serveur.trouverParAlias(alias2Saisi);
                        if (cible != null) {
                            cible.envoyer("DECLINE " + moiUC);
                        }
                        break;
                    }

                    // 3) Aucune invitation trouvée
                    cnx.envoyer("ERR AUCUNE INVITATION AVEC " + alias2SaisiUC);
                    break;
                }

                default: {
                    // Echo par défaut en MAJUSCULES
                    String arg = (evenement.getArgument() == null) ? "" : evenement.getArgument();
                    msg = (typeUC + " " + arg).toUpperCase();
                    cnx.envoyer(msg);
                    break;
                }



            }
        }
    }
}
