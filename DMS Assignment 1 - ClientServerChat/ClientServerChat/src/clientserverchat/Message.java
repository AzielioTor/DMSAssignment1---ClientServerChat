/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package clientserverchat;

import java.io.Serializable;

/**
 *
 * @author Adam Campbell - 14847097
 * @author Aziel Shaw - 1311607
 */
public abstract class Message implements Serializable {

    private String senderName;
    private String message;
    
    Message(String name, String message) {
        senderName = name;
        this.message = message;
    }
    
    public String getSenderName() {
        return senderName;
    }
    
    public String getMessage() {
        return message;
    }
}
