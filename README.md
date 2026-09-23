# github-org-management

CLI to generate stats and issues for a GitHub org.


## Generate keytab

To connect to the Red Hat VPN, a Kerb token is required.

```bash
ktutil
-> addent -password -p gahealy@IPA.REDHAT.COM -k 1 -e aes256-cts-hmac-sha1-96 -f
-> wkt /Users/gahealy/.kerberos.gahealy.keytab
-> quit

klist -kte ~/.kerberos.gahealy.keytab
kinit -n @IPA.REDHAT.COM -c FILE:$HOME/.krb5cc_gahealy
kinit -T FILE:$HOME/.krb5cc_gahealy -k -t ~/.kerberos.gahealy.keytab gahealy@IPA.REDHAT.COM
```

## Build

Both JVM and Native mode are supported. 

```bash
./mvnw clean install
./mvnw clean install -Pnative
```

Which allows you to run via:

```bash
./target/github-org-management-*-runner help
java -jar target/quarkus-app/quarkus-run.jar help
```

## GitHub Auth

Read permissions are required for the OAuth PAT.

```bash
export GITHUB_LOGIN=replace
export GITHUB_OAUTH=replace
```

## LDAP Lookup

```bash
export KRB5CCNAME=FILE:/tmp/krb5cc_$(id -u)
kinit gahealy@IPA.REDHAT.COM

ldapsearch -Y GSSAPI -H ldap:///dc%3Dipa%2Cdc%3Dredhat%2Cdc%3Dcom -b cn=users,cn=accounts,dc=ipa,dc=redhat,dc=com -s sub 'uid=gahealy'
ldapsearch -Y GSSAPI -H ldap:///dc%3Dipa%2Cdc%3Dredhat%2Cdc%3Dcom -b cn=users,cn=accounts,dc=ipa,dc=redhat,dc=com -s sub 'rhatSocialURL=Github->*garethahealy*'
```

## APIs

Once you've built the code, you can execute the commands, for example:

```bash
./target/github-org-management-*-runner users collect-members-from-ldap --organization=redhat-cop --csv-output=ldap-members.csv --ldap-members-csv=ldap-members.csv --fail-if-no-vpn=true --guess=false
```

For a full list of commands, see: [docs](docs)
