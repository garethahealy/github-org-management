package com.garethahealy.githuborgmanagement.testutils;

import com.garethahealy.githuborgmanagement.factories.LdapConnectionFactory;

public abstract class BaseRequiresLdapConnection {

    private final LdapConnectionFactory ldapConnectionFactory;

    public BaseRequiresLdapConnection(LdapConnectionFactory ldapConnectionFactory) {
        this.ldapConnectionFactory = ldapConnectionFactory;
    }

    protected boolean canConnectVpn() {
        return ldapConnectionFactory.canConnect();
    }
}
