package com.chat.serveur;
import java.util.Vector;

import com.commun.net.Connexion;

/**
 * Serveur de chat (public + privé).
 */
public class ServeurChat extends Serveur {

    public ServeurChat(int port) {
        super(port);
    }

    @Override
    public synchronized boolean ajouter(Connexion connexion) {
        String hist = this.historique();
        if (!hist.isEmpty()) {
            connexion.envoyer("HIST " + hist);
        }
        boolean added = super.ajouter(connexion);

        // 👋 Message d'accueil personnalisé
        connexion.envoyer("Hello, " + connexion.getAlias());

        return added;
    }

    @Override
    protected boolean validerConnexion(Connexion connexion) {
        String aliasFourni = connexion.getAvailableText().trim();
        char c;
        int taille;
        boolean res = true;
        if ("".equals(aliasFourni)) {
            return false;
        }
        taille = aliasFourni.length();
        for (int i=0;i<taille;i++) {
            c = aliasFourni.charAt(i);
            if ((c<'a' || c>'z') && (c<'A' || c>'Z') && (c<'0' || c>'9')
                    && c!='_' && c!='-') {
                res = false;
                break;
            }
        }
        if (!res)
            return false;
        for (Connexion cnx:connectes) {
            if (aliasFourni.equalsIgnoreCase(cnx.getAlias())) { // alias déjà utilisé
                res = false;
                break;
            }
        }
        if (!res)
            return false;
        connexion.setAlias(aliasFourni);
        return true;
    }

    /** Liste des connectés au format alias1:alias2:...: */
    public String list() {
        String s = "";
        for (Connexion cnx:connectes)
            s+=cnx.getAlias()+":";
        return s;
    }

    /** Historique (message1\nmessage2\n...) */
    public String historique() {
        return historiquePayload();
    }

    /** Envoie à tous sauf l’expéditeur + ajoute à l’historique */
    public void envoyerATousSauf(String str, String aliasExpediteur){
        ajouterHistorique(aliasExpediteur, str);
        for (Connexion cnx:connectes){
            if (!cnx.getAlias().equals(aliasExpediteur)){
                cnx.envoyer(aliasExpediteur + " >>" + str);
            }
        }
    }

    // -------------------- Historique public --------------------
    public final Vector<String> historique = new Vector<>();

    /** Ajoute "alias>>message" (sans \n) */
    public void ajouterHistorique(String alias, String message) {
        if (alias == null) alias = "";
        if (message == null) message = "";
        message = message.replace('\n', ' ');
        historique.add(alias + ">>" + message);
    }

    /** Concatène l’historique avec '\n' */
    public String historiquePayload() {
        if (historique.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < historique.size(); i++) {
            if (i > 0) sb.append('\n');
            sb.append(historique.get(i));
        }
        return sb.toString();
    }

    // -------------------- Invitations & Salons --------------------
    private final Vector<Invitation> invitations = new Vector<>();
    private final Vector<SalonPrive> salons = new Vector<>();

    /** Trouve une connexion par alias (ignore la casse) */
    public Connexion trouverParAlias(String alias) {
        if (alias == null) return null;
        for (Connexion c : connectes) {
            if (c.getAlias().equalsIgnoreCase(alias)) return c;
        }
        return null;
    }

    /** Alias "canonique" (casse exacte enregistrée côté serveur) */
    private String canon(String alias) {
        if (alias == null) return "";
        String a = alias.trim();
        for (Connexion c : connectes) {
            if (c.getAlias().equalsIgnoreCase(a)) {
                return c.getAlias();
            }
        }
        return a;
    }

    // ----- Invitations (ordre sensible: hôte -> invité), comparaison ignoreCase -----
    public synchronized boolean existeInvitation(String hote, String invite) {
        String h = canon(hote), i = canon(invite);
        for (Invitation inv : invitations) {
            if (inv.getHote() != null && inv.getInvite() != null
                    && inv.getHote().equalsIgnoreCase(h)
                    && inv.getInvite().equalsIgnoreCase(i)) {
                return true;
            }
        }
        return false;
    }

    public synchronized void ajouterInvitation(String hote, String invite) {
        String h = canon(hote), i = canon(invite);
        if (!existeInvitation(h, i)) {
            invitations.add(new Invitation(h, i)); // on stocke canonique
        }
    }

    public synchronized void supprimerInvitation(String hote, String invite) {
        String h = canon(hote), i = canon(invite);
        for (int idx = 0; idx < invitations.size(); idx++) {
            Invitation inv = invitations.get(idx);
            if (inv.getHote() != null && inv.getInvite() != null
                    && inv.getHote().equalsIgnoreCase(h)
                    && inv.getInvite().equalsIgnoreCase(i)) {
                invitations.remove(idx);
                break;
            }
        }
    }

    // ----- Salons (ordre insensible: A-B == B-A) -----
    public synchronized boolean existeSalon(String a, String b) {
        return salons.contains(new SalonPrive(a, b));
    }

    public synchronized boolean ajouterSalon(String a, String b) {
        SalonPrive sp = new SalonPrive(a, b);
        if (!salons.contains(sp)) {
            salons.add(sp);
            return true;
        }
        return false;
    }

    public synchronized boolean supprimerSalon(String a, String b) {
        return salons.remove(new SalonPrive(a, b));
    }

    // Retourne "A:B:C" = liste des hôtes qui ont invité aliasInvite
    public synchronized String invitationsRecues(String aliasInvite) {
        if (aliasInvite == null) return "";
        String target = aliasInvite.trim();
        StringBuilder sb = new StringBuilder();
        for (Invitation inv : invitations) {
            if (inv.getInvite() != null && inv.getInvite().equalsIgnoreCase(target)) {
                String hote = inv.getHote();
                if (hote != null && !hote.isEmpty()) {
                    if (sb.length() > 0) sb.append(':');
                    sb.append(hote.toUpperCase()); // normalisation d'affichage
                }
            }
        }
        return sb.toString();
    }

    // Nettoie toutes les invitations et salons liés à 'alias' (et notifie les partenaires)
    public synchronized void nettoyerDeconnexion(String alias) {
        if (alias == null) return;

        // --- Supprimer les invitations où alias est hôte ou invité ---
        for (int i = 0; i < invitations.size(); i++) {
            Invitation inv = invitations.get(i);
            if (inv.getHote() != null && inv.getHote().equalsIgnoreCase(alias)
                    || inv.getInvite() != null && inv.getInvite().equalsIgnoreCase(alias)) {
                invitations.remove(i);
                i--;
            }
        }

        // --- Fermer les salons privés impliquant alias et prévenir l'autre partie ---
        String aliasUC = alias.toUpperCase();
        for (int i = 0; i < salons.size(); i++) {
            SalonPrive sp = salons.get(i);
            String a = sp.getHote();
            String b = sp.getInvite();
            if (a != null && b != null
                    && (a.equalsIgnoreCase(alias) || b.equalsIgnoreCase(alias))) {
                // Trouver l'autre participant
                String autre = a.equalsIgnoreCase(alias) ? b : a;
                Connexion cAutre = trouverParAlias(autre);
                if (cAutre != null) {
                    // Le client affiche déjà: "<arg> a quitté le salon privé."
                    cAutre.envoyer("QUIT " + aliasUC);
                }
                salons.remove(i);
                i--;
            }
        }
    }



}
