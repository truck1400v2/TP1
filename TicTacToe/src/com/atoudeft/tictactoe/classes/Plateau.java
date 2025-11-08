package com.atoudeft.tictactoe.classes;

import com.atoudeft.tictactoe.MethodeNonImplementeeException;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public final class Plateau {
    private final Symbole[][] grille = new Symbole[3][3];
    private int casesRemplies = 0;

    public Symbole get(int ligne, int colonne) {
        return grille[ligne][colonne];
    }

    public boolean estVide(Position p) {
        return grille[p.getLigne()][p.getColonne()] == null;
    }

    public int getNombreCasesRemplies() {
        return casesRemplies;
    }

    public boolean estPlein() {
        return casesRemplies == 9;
    }

    // 4.3 : tente de réaliser le coup reçu ; false si la case est pleine
    public boolean placer(Coup coup) {
        Position p = coup.getPosition();
        int i = p.getLigne();
        int j = p.getColonne();

        // Case déjà occupée → false
        if (grille[i][j] != null) {
            return false;
        }

        // Place le symbole et incrémente le nombre de cases remplies
        grille[i][j] = coup.getSymbole();
        casesRemplies++;
        return true;
    }

    // 4.4 : vérifie s'il existe une ligne/colonne/diagonale gagnante
    public List<Position> ligneGagnante() {
        int[][][] lignes = {
            {{0,0},{0,1},{0,2}}, {{1,0},{1,1},{1,2}}, {{2,0},{2,1},{2,2}},
            {{0,0},{1,0},{2,0}}, {{0,1},{1,1},{2,1}}, {{0,2},{1,2},{2,2}},
            {{0,0},{1,1},{2,2}}, {{0,2},{1,1},{2,0}}
        };

        List<Position> gagnante = new ArrayList<>(3);

        for (int[][] ligne : lignes) {
            Symbole s1 = grille[ligne[0][0]][ligne[0][1]];
            Symbole s2 = grille[ligne[1][0]][ligne[1][1]];
            Symbole s3 = grille[ligne[2][0]][ligne[2][1]];

            if (s1 != null && s1 == s2 && s2 == s3) {
                gagnante.add(new Position(ligne[0][0], ligne[0][1]));
                gagnante.add(new Position(ligne[1][0], ligne[1][1]));
                gagnante.add(new Position(ligne[2][0], ligne[2][1]));
                return gagnante;
            }
        }

        return Collections.emptyList();
    }

    // Pour 4.1 : affichage du plateau utilisé par Partie.toString()
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("   0 1 2\n");
        for (int i = 0; i < 3; i++) {
            sb.append(i).append("  ");
            for (int j = 0; j < 3; j++) {
                Symbole s = grille[i][j];
                sb.append(s == null ? '.' : s.name());
                if (j < 2) sb.append(' ');
            }
            if (i < 2) sb.append('\n');
        }
        return sb.toString();
    }
}
