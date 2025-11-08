package com.atoudeft.tictactoe.classes;

import com.atoudeft.tictactoe.MethodeNonImplementeeException;

public final class Partie {
    private final Plateau plateau = new Plateau();
    private Symbole joueurCourant;
    private StatutPartie statut;

    public Plateau getPlateau() { return plateau; }
    public Symbole getJoueurCourant() { return joueurCourant; }
    public StatutPartie getStatut() { return statut; }

    // Constructeur avec joueur courant
    public Partie(Symbole joueurCourant) {
        this.joueurCourant = joueurCourant;
        this.statut = StatutPartie.EN_COURS;
    }

    // Constructeur par défaut utilisé par Demo (X commence)
    public Partie() {
        this(Symbole.X);
    }

    // 4.6 : jouer un coup
    public boolean jouer(Symbole symbole, Position position) {
        // Si la partie est terminée, on ne peut plus jouer
        if (statut != StatutPartie.EN_COURS) {
            return false;
        }

        // Si ce n'est pas le symbole du joueur courant, on refuse
        if (symbole != joueurCourant) {
            return false;
        }

        // On tente de placer le coup
        Coup coup = new Coup(position, symbole);
        if (!plateau.placer(coup)) {
            // Case déjà pleine
            return false;
        }

        // Mise à jour du statut
        mettreAJourStatutApresCoup();

        // Si la partie continue, on change de joueur courant
        if (statut == StatutPartie.EN_COURS) {
            joueurCourant = (joueurCourant == Symbole.X) ? Symbole.O : Symbole.X;
        }

        return true;
    }

    public boolean isPartieEnCours() { return statut == StatutPartie.EN_COURS; }

    // 4.5 : met à jour le statut après un coup joué
    private void mettreAJourStatutApresCoup() {
        java.util.List<Position> gagnante = plateau.ligneGagnante();

        // S'il y a une ligne gagnante
        if (!gagnante.isEmpty()) {
            Position p0 = gagnante.get(0);
            Symbole s = plateau.get(p0.getLigne(), p0.getColonne());
            if (s == Symbole.X) {
                statut = StatutPartie.X_GAGNE;
            } else if (s == Symbole.O) {
                statut = StatutPartie.O_GAGNE;
            }
            return;
        }

        // Si pas de gagnant mais plateau plein → partie nulle
        if (plateau.estPlein()) {
            statut = StatutPartie.NULLE;
        }
        // Sinon : statut reste EN_COURS
    }

    @Override
    public String toString() {
        return plateau + "\n" +
               "Joueur Courant : " + joueurCourant + "\n" +
               "Etat : " + statut + "\n";
    }
}
