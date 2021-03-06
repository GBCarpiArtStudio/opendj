/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyright [year] [name of copyright owner]".
 *
 * Copyright 2006-2008 Sun Microsystems, Inc.
 * Portions Copyright 2013-2016 ForgeRock AS.
 */
package org.opends.server.extensions;

import org.forgerock.opendj.ldap.ByteString;
import org.forgerock.opendj.ldap.ByteStringBuilder;
import org.forgerock.opendj.ldap.ResultCode;
import org.forgerock.opendj.ldap.SearchScope;
import org.opends.server.TestCaseUtils;
import org.opends.server.api.SASLMechanismHandler;
import org.opends.server.core.BindOperation;
import org.opends.server.core.DirectoryServer;
import org.opends.server.protocols.internal.InternalClientConnection;
import org.opends.server.protocols.internal.InternalSearchOperation;
import org.opends.server.protocols.internal.Requests;
import org.opends.server.protocols.internal.SearchRequest;
import org.opends.server.schema.SchemaConstants;
import org.opends.server.tools.LDAPSearch;
import org.opends.server.types.AuthenticationInfo;
import org.forgerock.opendj.ldap.DN;
import org.opends.server.types.Entry;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.opends.server.protocols.internal.InternalClientConnection.*;
import static org.testng.Assert.*;

/**
 * A set of test cases for the PLAIN SASL mechanism handler.
 */
public class PlainSASLMechanismHandlerTestCase
       extends ExtensionsTestCase
{
  /**
   * Ensures that the Directory Server is running.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @BeforeClass
  public void startServer()
         throws Exception
  {
    TestCaseUtils.startServer();
  }



  /**
   * Tests to ensure that the SASL PLAIN mechanism is loaded and available in
   * the server, and that it reports that it is password based and not secure.
   */
  @Test
  public void testSASLPlainLoaded()
  {
    SASLMechanismHandler<?> handler = DirectoryServer.getSASLMechanismHandler("PLAIN");
    assertNotNull(handler);

    assertTrue(handler.isPasswordBased("PLAIN"));
    assertFalse(handler.isSecure("PLAIN"));
  }



  /**
   * Tests to ensure that PLAIN is advertised as a supported SASL mechanism.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test
  public void testSASLPlainAdvertised() throws Exception
  {
    SearchRequest request =
        Requests.newSearchRequest(DN.rootDN(), SearchScope.BASE_OBJECT, "(supportedSASLMechanisms=PLAIN)");
    InternalSearchOperation op = getRootConnection().processSearch(request);
    assertFalse(op.getSearchEntries().isEmpty());
  }




  /**
   * Retrieves a set of passwords that may be used to test the password storage
   * scheme.
   *
   * @return  A set of passwords that may be used to test the password storage
   *          scheme.
   */
  @DataProvider(name = "testPasswords")
  public Object[][] getTestPasswords()
  {
    return new Object[][]
    {
      new Object[] { ByteString.valueOfUtf8("a") },
      new Object[] { ByteString.valueOfUtf8("ab") },
      new Object[] { ByteString.valueOfUtf8("abc") },
      new Object[] { ByteString.valueOfUtf8("abcd") },
      new Object[] { ByteString.valueOfUtf8("abcde") },
      new Object[] { ByteString.valueOfUtf8("abcdef") },
      new Object[] { ByteString.valueOfUtf8("abcdefg") },
      new Object[] { ByteString.valueOfUtf8("abcdefgh") },
      new Object[] { ByteString.valueOfUtf8("The Quick Brown Fox Jumps Over " +
                                         "The Lazy Dog") },
    };
  }



  /**
   * Creates a test user and authenticates to the server as that user with the
   * SASL PLAIN mechanism using a raw authentication ID (i.e., not prefixed by
   * either "u:" or "dn:").
   *
   * @param  password  The password for the user.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test(dataProvider = "testPasswords")
  public void testSASLPlainRawAuthID(ByteString password)
         throws Exception
  {
    TestCaseUtils.initializeTestBackend(true);
    TestCaseUtils.addEntry(
                   "dn: uid=test.user,o=test",
                   "objectClass: top",
                   "objectClass: person",
                   "objectClass: organizationalPerson",
                   "objectClass: inetOrgPerson",
                   "uid: test.user",
                   "givenName: Test",
                   "sn: User",
                   "cn: Test User",
                   "userPassword: " + password);


    ByteStringBuilder saslCredBytes = new ByteStringBuilder();
    saslCredBytes.appendByte(0);
    saslCredBytes.appendUtf8("test.user");
    saslCredBytes.appendByte(0);
    saslCredBytes.appendBytes(password);
    InternalClientConnection anonymousConn =
         new InternalClientConnection(new AuthenticationInfo());
    BindOperation bindOperation =
         anonymousConn.processSASLBind(ByteString.empty(), "PLAIN",
                                       saslCredBytes.toByteString());
    assertEquals(bindOperation.getResultCode(), ResultCode.SUCCESS);
  }



  /**
   * Creates a test user and authenticates to the server as that user with the
   * SASL PLAIN mechanism using the "u:" style authentication ID.
   *
   * @param  password  The password for the user.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test(dataProvider = "testPasswords")
  public void testSASLPlainUColon(ByteString password)
         throws Exception
  {
    TestCaseUtils.initializeTestBackend(true);
    TestCaseUtils.addEntry(
                   "dn: uid=test.user,o=test",
                   "objectClass: top",
                   "objectClass: person",
                   "objectClass: organizationalPerson",
                   "objectClass: inetOrgPerson",
                   "uid: test.user",
                   "givenName: Test",
                   "sn: User",
                   "cn: Test User",
                   "userPassword: " + password);


    ByteStringBuilder saslCredBytes = new ByteStringBuilder();
    saslCredBytes.appendByte(0);
    saslCredBytes.appendUtf8("u:test.user");
    saslCredBytes.appendByte(0);
    saslCredBytes.appendBytes(password);
    InternalClientConnection anonymousConn =
         new InternalClientConnection(new AuthenticationInfo());
    BindOperation bindOperation =
         anonymousConn.processSASLBind(ByteString.empty(), "PLAIN",
                                       saslCredBytes.toByteString());
    assertEquals(bindOperation.getResultCode(), ResultCode.SUCCESS);
  }



  /**
   * Creates a test user and authenticates to the server as that user with the
   * SASL PLAIN mechanism using the "u:" style authentication ID and
   * authorization ID.
   *
   * @param  password  The password for the user.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test(dataProvider = "testPasswords")
  public void testSASLPlainUColonWithAuthZID(ByteString password)
         throws Exception
  {
    TestCaseUtils.initializeTestBackend(true);
    TestCaseUtils.addEntry(
                   "dn: uid=test.user,o=test",
                   "objectClass: top",
                   "objectClass: person",
                   "objectClass: organizationalPerson",
                   "objectClass: inetOrgPerson",
                   "uid: test.user",
                   "givenName: Test",
                   "sn: User",
                   "cn: Test User",
                   "userPassword: " + password);


    ByteStringBuilder saslCredBytes = new ByteStringBuilder();
    saslCredBytes.appendUtf8("u:test.user");
    saslCredBytes.appendByte(0);
    saslCredBytes.appendUtf8("u:test.user");
    saslCredBytes.appendByte(0);
    saslCredBytes.appendBytes(password);
    InternalClientConnection anonymousConn =
         new InternalClientConnection(new AuthenticationInfo());
    BindOperation bindOperation =
         anonymousConn.processSASLBind(ByteString.empty(), "PLAIN",
                                       saslCredBytes.toByteString());
    assertEquals(bindOperation.getResultCode(), ResultCode.SUCCESS);
  }



  /**
   * Creates a test user and authenticates to the server as that user with the
   * SASL PLAIN mechanism using the "dn:" style authentication ID.
   *
   * @param  password  The password for the user.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test(dataProvider = "testPasswords")
  public void testSASLPlainDNColon(ByteString password)
         throws Exception
  {
    TestCaseUtils.initializeTestBackend(true);
    Entry e = TestCaseUtils.addEntry(
                   "dn: uid=test.user,o=test",
                   "objectClass: top",
                   "objectClass: person",
                   "objectClass: organizationalPerson",
                   "objectClass: inetOrgPerson",
                   "uid: test.user",
                   "givenName: Test",
                   "sn: User",
                   "cn: Test User",
                   "userPassword: " + password);


    ByteStringBuilder saslCredBytes = new ByteStringBuilder();
    saslCredBytes.appendByte(0);
    saslCredBytes.appendUtf8("dn:");
    saslCredBytes.appendUtf8(e.getName().toString());
    saslCredBytes.appendByte(0);
    saslCredBytes.appendBytes(password);
    InternalClientConnection anonymousConn =
         new InternalClientConnection(new AuthenticationInfo());
    BindOperation bindOperation =
         anonymousConn.processSASLBind(ByteString.empty(), "PLAIN",
                                       saslCredBytes.toByteString());
    assertEquals(bindOperation.getResultCode(), ResultCode.SUCCESS);
  }



  /**
   * Creates a test user and authenticates to the server as that user with the
   * SASL PLAIN mechanism using the "dn:" style authentication ID and an
   * authorization ID.
   *
   * @param  password  The password for the user.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test(dataProvider = "testPasswords")
  public void testSASLPlainDNColonWithAuthZID(ByteString password)
         throws Exception
  {
    TestCaseUtils.initializeTestBackend(true);
    Entry e = TestCaseUtils.addEntry(
                   "dn: uid=test.user,o=test",
                   "objectClass: top",
                   "objectClass: person",
                   "objectClass: organizationalPerson",
                   "objectClass: inetOrgPerson",
                   "uid: test.user",
                   "givenName: Test",
                   "sn: User",
                   "cn: Test User",
                   "userPassword: " + password);


    ByteStringBuilder saslCredBytes = new ByteStringBuilder();
    saslCredBytes.appendUtf8("dn:");
    saslCredBytes.appendUtf8(e.getName().toString());
    saslCredBytes.appendByte(0);
    saslCredBytes.appendUtf8("dn:");
    saslCredBytes.appendUtf8(e.getName().toString());
    saslCredBytes.appendByte(0);
    saslCredBytes.appendBytes(password);
    InternalClientConnection anonymousConn =
         new InternalClientConnection(new AuthenticationInfo());
    BindOperation bindOperation =
         anonymousConn.processSASLBind(ByteString.empty(), "PLAIN",
                                       saslCredBytes.toByteString());
    assertEquals(bindOperation.getResultCode(), ResultCode.SUCCESS);
  }



  /**
   * Ensures that SASL PLAIN authentication will work for root users.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test
  public void testSASLPlainAsRoot()
         throws Exception
  {
    ByteString rootCreds =
         ByteString.valueOfUtf8("\u0000dn:cn=Directory Manager\u0000password");

    InternalClientConnection anonymousConn =
         new InternalClientConnection(new AuthenticationInfo());
    BindOperation bindOperation =
         anonymousConn.processSASLBind(ByteString.empty(), "PLAIN",
                                    rootCreds);
    assertEquals(bindOperation.getResultCode(), ResultCode.SUCCESS);
  }



  /**
   * Ensures that SASL PLAIN authentication works over LDAP as well as via the
   * internal protocol.  The authentication will be performed as the root user.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test
  public void testSASLPlainOverLDAP()
         throws Exception
  {
    String[] args =
    {
      "--noPropertiesFile",
      "-h", "127.0.0.1",
      "-p", String.valueOf(TestCaseUtils.getServerLdapPort()),
      "-o", "mech=PLAIN",
      "-o", "authid=dn:cn=Directory Manager",
      "-w", "password",
      "-b", "",
      "-s", "base",
      "(objectClass=*)",
      SchemaConstants.NO_ATTRIBUTES
    };

    assertEquals(LDAPSearch.mainSearch(args, false, null, System.err), 0);
  }



  /**
   * Retrieves sets of invalid credentials that will not succeed when using
   * SASL PLAIN.
   *
   * @return  Sets of invalid credentials that will not work when using SASL
   * PLAIN.
   */
  @DataProvider(name = "invalidCredentials")
  public Object[][] getInvalidCredentials()
  {
    return new Object[][]
    {
      new Object[] { null },
      new Object[] { ByteString.empty() },
      new Object[] { ByteString.valueOfUtf8("u:test.user") },
      new Object[] { ByteString.valueOfUtf8("password") },
      new Object[] { ByteString.valueOfUtf8("\u0000") },
      new Object[] { ByteString.valueOfUtf8("\u0000\u0000") },
      new Object[] { ByteString.valueOfUtf8("\u0000password") },
      new Object[] { ByteString.valueOfUtf8("\u0000\u0000password") },
      new Object[] { ByteString.valueOfUtf8("\u0000u:test.user\u0000") },
      new Object[] { ByteString.valueOfUtf8("\u0000dn:\u0000password") },
      new Object[] { ByteString.valueOfUtf8("\u0000dn:bogus\u0000password") },
      new Object[] { ByteString.valueOfUtf8("\u0000dn:cn=no such user" +
                                         "\u0000password") },
      new Object[] { ByteString.valueOfUtf8("\u0000u:\u0000password") },
      new Object[] { ByteString.valueOfUtf8("\u0000u:nosuchuser\u0000password") },
      new Object[] { ByteString.valueOfUtf8("\u0000u:test.user\u0000" +
                                         "wrongpassword") },
    };
  }



  /**
   * Creates a test user and authenticates to the server as that user with the
   * SASL PLAIN mechanism using the "dn:" style authentication ID.
   *
   * @param  saslCredentials  The (invalid) SASL credentials to use.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test(dataProvider = "invalidCredentials")
  public void testInvalidCredentials(ByteString saslCredentials)
         throws Exception
  {
    TestCaseUtils.initializeTestBackend(true);
    TestCaseUtils.addEntry(
                   "dn: uid=test.user,o=test",
                   "objectClass: top",
                   "objectClass: person",
                   "objectClass: organizationalPerson",
                   "objectClass: inetOrgPerson",
                   "uid: test.user",
                   "givenName: Test",
                   "sn: User",
                   "cn: Test User",
                   "userPassword: password");

    InternalClientConnection anonymousConn =
         new InternalClientConnection(new AuthenticationInfo());
    BindOperation bindOperation =
         anonymousConn.processSASLBind(ByteString.empty(), "PLAIN",
                                       saslCredentials);
    assertEquals(bindOperation.getResultCode(), ResultCode.INVALID_CREDENTIALS);
  }



  /**
   * Performs a failed LDAP bind using PLAIN with an authorization ID that
   * contains the DN of an entry that doesn't exist.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test
  public void testLDAPBindFailNonexistentAuthzDN()
         throws Exception
  {
    TestCaseUtils.initializeTestBackend(true);
    TestCaseUtils.addEntry(
      "dn: uid=test.user,o=test",
      "objectClass: top",
      "objectClass: person",
      "objectClass: organizationalPerson",
      "objectClass: inetOrgPerson",
      "uid: test.user",
      "givenName: Test",
      "sn: User",
      "cn: Test User",
      "userPassword: password",
      "ds-privilege-name: proxied-auth");

    String[] args =
    {
      "--noPropertiesFile",
      "-h", "127.0.0.1",
      "-p", String.valueOf(TestCaseUtils.getServerLdapPort()),
      "-o", "mech=PLAIN",
      "-o", "authid=dn:uid=test.user,o=test",
      "-o", "authzid=dn:uid=nonexistent,o=test",
      "-w", "password",
      "-b", "",
      "-s", "base",
      "(objectClass=*)"
    };
    assertFalse(LDAPSearch.mainSearch(args, false, null, null) == 0);
  }



  /**
   * Performs a failed LDAP bind using PLAIN with an authorization ID that
   * contains a username for an entry that doesn't exist.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test
  public void testLDAPBindFailNonexistentAuthzUsername()
         throws Exception
  {
    TestCaseUtils.initializeTestBackend(true);
    TestCaseUtils.addEntry(
      "dn: uid=test.user,o=test",
      "objectClass: top",
      "objectClass: person",
      "objectClass: organizationalPerson",
      "objectClass: inetOrgPerson",
      "uid: test.user",
      "givenName: Test",
      "sn: User",
      "cn: Test User",
      "userPassword: password",
      "ds-privilege-name: proxied-auth");

    String[] args =
    {
      "--noPropertiesFile",
      "-h", "127.0.0.1",
      "-p", String.valueOf(TestCaseUtils.getServerLdapPort()),
      "-o", "mech=PLAIN",
      "-o", "authid=dn:uid=test.user,o=test",
      "-o", "authzid=u:nonexistent",
      "-w", "password",
      "-b", "",
      "-s", "base",
      "(objectClass=*)"
    };
    assertFalse(LDAPSearch.mainSearch(args, false, null, null) == 0);
  }



  /**
   * Performs a failed LDAP bind using PLAIN with an authorization ID that
   * contains a malformed DN.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test
  public void testLDAPBindFailMalformedAuthzDN()
         throws Exception
  {
    TestCaseUtils.initializeTestBackend(true);
    TestCaseUtils.addEntry(
      "dn: uid=test.user,o=test",
      "objectClass: top",
      "objectClass: person",
      "objectClass: organizationalPerson",
      "objectClass: inetOrgPerson",
      "uid: test.user",
      "givenName: Test",
      "sn: User",
      "cn: Test User",
      "userPassword: password",
      "ds-privilege-name: proxied-auth");

    String[] args =
    {
      "--noPropertiesFile",
      "-h", "127.0.0.1",
      "-p", String.valueOf(TestCaseUtils.getServerLdapPort()),
      "-o", "mech=PLAIN",
      "-o", "authid=dn:uid=test.user,o=test",
      "-o", "authzid=dn:malformed",
      "-w", "password",
      "-b", "",
      "-s", "base",
      "(objectClass=*)"
    };
    assertFalse(LDAPSearch.mainSearch(args, false, null, null) == 0);
  }
}

