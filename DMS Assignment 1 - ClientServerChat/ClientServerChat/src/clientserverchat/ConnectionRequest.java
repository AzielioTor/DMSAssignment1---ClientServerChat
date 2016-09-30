/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package clientserverchat;

/**
 *
 * @author Adam Campbell - 14847097
 * @author Aziel Shaw - 1311607
 */
public class ConnectionRequest extends Message {

    ConnectionRequest(String name) {
        super(name, name + " has connected.");
    }
}