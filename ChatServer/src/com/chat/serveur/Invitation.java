package com.chat.serveur;

/** Invitation hôte -> invité (ordre sensible) */
public class Invitation {
    private final String hote;
    private final String invite;

    public Invitation(String hote, String invite) {
        this.hote = hote;
        this.invite = invite;
    }

    public String getHote()   { return hote; }
    public String getInvite() { return invite; }

    // Ordre sensible, comparaison ignoreCase
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Invitation)) return false;
        Invitation that = (Invitation) o;
        return hote != null && invite != null
                && that.hote != null && that.invite != null
                && hote.equalsIgnoreCase(that.hote)
                && invite.equalsIgnoreCase(that.invite);
    }

    @Override
    public int hashCode() { //Compare objts
        String h = (hote == null) ? "" : hote.toLowerCase();
        String i = (invite == null) ? "" : invite.toLowerCase();
        return (h + "\u0000" + i).hashCode();
    }

    @Override
    public String toString() { return hote + "->" + invite; }
}
