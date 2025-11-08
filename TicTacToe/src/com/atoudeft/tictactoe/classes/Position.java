package com.atoudeft.tictactoe.classes;

public final class Position {
    private final int ligne;
    private final int colonne;

    public Position(int ligne, int colonne) {
        if (ligne < 0 || ligne > 2 || colonne < 0 || colonne > 2) {
            throw new IllegalArgumentException("Position hors plateau: (" + ligne + "," + colonne + ")");
        }
        this.ligne = ligne;
        this.colonne = colonne;
    }

    public int getLigne()   { return ligne; }
    public int getColonne() { return colonne; }

    @Override
    public String toString() {
        return "(" + ligne + "," + colonne + ")";
    }

    // 4.2 : Deux positions sont égales si même ligne et même colonne
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Position that = (Position) o;
        return ligne == that.ligne && colonne == that.colonne;
    }

    @Override
    public int hashCode() {
        int result = Integer.hashCode(ligne);
        result = 31 * result + Integer.hashCode(colonne);
        return result;
    }
}
