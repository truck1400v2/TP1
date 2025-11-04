package com.chat.serveur;

/** Salon privé entre deux alias (ordre insensible A-B == B-A) */
public class SalonPrive  {

    private String hote;
    private String invite;

    public SalonPrive(String hote,String invite){
        this.hote = hote;
        this.invite = invite;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SalonPrive)) return false;
        SalonPrive other = (SalonPrive) o;

        if (this.hote == null || this.invite == null || other.hote == null || other.invite == null) {
            return false;
        }

        // direct : (hote, invite)
        if (this.hote.equalsIgnoreCase(other.hote) &&
                this.invite.equalsIgnoreCase(other.invite)) {
            return true;
        }
        // inverse : (invite, hote)
        return this.hote.equalsIgnoreCase(other.invite) &&
                this.invite.equalsIgnoreCase(other.hote);
    }

    @Override
    public int hashCode() {
        String a = (hote == null) ? "" : hote.toLowerCase();
        String b = (invite == null) ? "" : invite.toLowerCase();
        if (a.compareTo(b) > 0) { String t = a; a = b; b = t; }
        return (a + ":" + b).hashCode();
    }

    public String getInvite() { return invite; }
    public void setInvite(String invite) { this.invite = invite; }
    public String getHote() { return hote; }
    public void setHote(String hote) { this.hote = hote; }
}
