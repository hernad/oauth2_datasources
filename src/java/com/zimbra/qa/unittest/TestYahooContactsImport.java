package com.zimbra.qa.unittest;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.zimbra.client.ZContact;
import com.zimbra.client.ZDataSource;
import com.zimbra.client.ZFolder;
import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZMailbox.ContactSortBy;
import com.zimbra.common.account.Key;
import com.zimbra.common.localconfig.LC;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.datasource.DataSourceManager;
/**
 * @author Greg Solovyev
 *
 */
public class TestYahooContactsImport {
    @Rule
    public TestName testInfo = new TestName();
    private String clientId = null;
    private String clientSecret = null;
    private String yahooTestAccount = null;
    private String testToken = null;
    private static String USER_NAME = null;
    private String testId;
    private Account acct = null;

    @Before
    public void setUp() throws Exception {
        clientId = LC.get("zm_oauth_yahoo_client_id");
        TestUtil.assumeTrue("zm_oauth_yahoo_client_id is not set. This LC setting is required to use YahooContactsImport.", clientId != null && !clientId.isEmpty());
        clientSecret = LC.get("zm_oauth_yahoo_client_secret");
        TestUtil.assumeTrue("zm_oauth_yahoo_client_secret is not set. This LC setting is required to use YahooContactsImport.", clientSecret != null && !clientSecret.isEmpty());
        yahooTestAccount = LC.get("zm_yahoo_test_account");
        TestUtil.assumeTrue("zm_yahoo_test_account is not set. This LC setting is required to run the SOAP tests for YahooContactsImport.", yahooTestAccount != null && !yahooTestAccount.isEmpty());
        testToken = LC.get("zm_yahoo_test_refresh_token");
        TestUtil.assumeTrue("zm_yahoo_test_refresh_token is not set. This LC setting is required to run the SOAP tests for YahooContactsImport.", testToken != null && !testToken.isEmpty());
        testId = String.format("%s-%s-%d", this.getClass().getSimpleName(), testInfo.getMethodName(), (int)Math.abs(Math.random()*100));
        USER_NAME = String.format("%s-user", testId).toLowerCase();
        cleanUp();
        acct = TestUtil.createAccount(USER_NAME);
    }

    @After
    public void tearDown() throws Exception {
        cleanUp();
    }
    
    private void cleanUp() throws Exception {
        if (USER_NAME != null) {
            TestUtil.deleteAccountIfExists(USER_NAME);
        }
    }

    @Test
    public void testFirstSync() throws Exception {
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        ZFolder folder = TestUtil.createFolder(zmbox, "/testCustomDS");
        ZDataSource zds = new ZDataSource(yahooTestAccount, true);
        zds.setImportClass("com.synacor.zimbra.OAuthDataImport");
        zds.setRefreshToken(testToken);
        zds.setHost(ZDataSource.SOURCE_HOST_YAHOO);
        zds.setFolderId(folder.getId());
        String dsId = zmbox.createDataSource(zds);
        zds.setId(dsId);
        try {
            zmbox.importData(Arrays.asList(zds));
        } catch (Exception e) {
            fail("Should not throw an exception");
        }
        int found = waitForContacts(zmbox, folder, 10000, 1000, 1200);
        assertTrue("Expected to find 20 contacts. Found " + found, found >= 1200);
        DataSource dataSource = Provisioning.getInstance().get(acct, Key.DataSourceBy.name, yahooTestAccount);
        assertNotNull("DS should exist after initial sync", dataSource);
        assertEquals("Unexpected datasource import class name", "com.synacor.zimbra.OAuthDataImport", dataSource.getAttr(Provisioning.A_zimbraDataSourceImportClassName));
        assertEquals("Unexpected datasource refresh token", testToken, dataSource.getAttr(Provisioning.A_zimbraDataSourceOAuthRefreshToken));
        assertEquals("Unexpected datasource host", ZDataSource.SOURCE_HOST_YAHOO, dataSource.getAttr(Provisioning.A_zimbraDataSourceHost));
        assertEquals("Unexpected datasource folder ID",  Integer.parseInt(folder.getId()), dataSource.getFolderId());
        assertEquals("Unexpected datasource ID", dsId, dataSource.getId());
        String [] attrs = dataSource.getMultiAttr(Provisioning.A_zimbraDataSourceAttribute);
        assertEquals("expecting to find 1 value in zimbraDataSourceAttribute. Found " + attrs.length, 1, attrs.length);
        assertNotNull("zimbraDataSourceAttribute should NOT be empty after initial sync", attrs);
        String newRev = attrs[0];
        try {
            assertNotNull("zimbraDataSourceAttribute should has a non null value", newRev);
            int rev = Integer.parseInt(newRev);
            assertTrue("expecting a non 0 revision after initial sync", rev > 0);
        } catch (NumberFormatException e) {
            fail("Bad value in zimbraDataSourceAttribute " + newRev);
        }
    }

    @Test
    public void test() throws Exception {
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        ZFolder folder = TestUtil.createFolder(zmbox, "/testCustomDS");
        ZDataSource zds = new ZDataSource(yahooTestAccount, true);
        zds.setImportClass("com.synacor.zimbra.OAuthDataImport");
        zds.setRefreshToken(testToken);
        zds.setHost(ZDataSource.SOURCE_HOST_YAHOO);
        zds.setFolderId(folder.getId());
        String dsId = zmbox.createDataSource(zds);
        zds.setId(dsId);
        String result = zmbox.testDataSource(zds);
        assertNull("test should return null on success. Returned: " + result, result);
        DataSource dataSource = Provisioning.getInstance().get(acct, Key.DataSourceBy.name, yahooTestAccount);
        assertNotNull("DS should exist after test", dataSource);
        assertEquals("Unexpected datasource import class name", "com.synacor.zimbra.OAuthDataImport", dataSource.getAttr(Provisioning.A_zimbraDataSourceImportClassName));
        assertEquals("Unexpected datasource refresh token", testToken, dataSource.getAttr(Provisioning.A_zimbraDataSourceOAuthRefreshToken));
        assertEquals("Unexpected datasource host", ZDataSource.SOURCE_HOST_YAHOO, dataSource.getAttr(Provisioning.A_zimbraDataSourceHost));
        assertEquals("Unexpected datasource folder ID", Integer.parseInt(folder.getId()), dataSource.getFolderId());
        assertEquals("Unexpected datasource ID", dsId, dataSource.getId());
        String [] attrs = dataSource.getMultiAttr(Provisioning.A_zimbraDataSourceAttribute);
        assertEquals("zimbraDataSourceAttribute should be empty", 0, attrs.length);
    }

    @Test
    public void testInvalidSource() throws Exception {
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        ZFolder folder = TestUtil.createFolder(zmbox, "/testCustomDS");
        ZDataSource zds = new ZDataSource(yahooTestAccount, true);
        zds.setImportClass("com.synacor.zimbra.OAuthDataImport");
        zds.setRefreshToken(testToken);
        zds.setHost("unknown.host");
        zds.setFolderId(folder.getId());
        String dsId = zmbox.createDataSource(zds);
        zds.setId(dsId);
        String result = zmbox.testDataSource(zds);
        assertNotNull("DataImport::test should return non-null on error.", result);
        assertTrue("Expecting No known DataImport implementation for unknown.host in the error message", result.indexOf("No known DataImport implementation for unknown.host") >-1);
    }

    private int waitForContacts(ZMailbox zmbx, ZFolder folder, int timeout, int interval, int numContacts) throws Exception {
        int found = 0;
        while(timeout > 0) {
            List<ZContact> results = zmbx.getContactsForFolder(folder.getId(), null, ContactSortBy.nameAsc, false, null);
            found = results.size();
            if(found >= numContacts) {
                return results.size();
            }
            Thread.sleep(interval);
            timeout -= interval;
        }
        return found;
    }
}
