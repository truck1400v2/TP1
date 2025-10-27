package com.chat.serveur;

public class Invitation {

    private String hote;
    private String invite;


    public Invitation(String hote,String invite){
        hote = this.hote;
        invite = this.invite;

    }

    public String getInvite() {
        return invite;
    }

    public void setInvite(String invite) {
        this.invite = invite;
    }

    public String getHote() {
        return hote;
    }

    public void setHote(String hote) {
        this.hote = hote;
    }
}


