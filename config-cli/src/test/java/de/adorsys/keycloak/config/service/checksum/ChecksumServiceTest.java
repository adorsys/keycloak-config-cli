package de.adorsys.keycloak.config.service.checksum;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;

public class ChecksumServiceTest {

    private ChecksumService checksumService;

    @Before
    public void setup() throws Exception {
        checksumService = new ChecksumService();
    }

    @Test
    public void shouldThrowOnNullString() throws Exception {
        String nullString = null;

        catchException(checksumService).checksum(nullString);

        Assert.assertThat(caughtException(),
                allOf(
                        instanceOf(IllegalArgumentException.class)
                )
        );
    }

    @Test
    public void shouldThrowOnNullBytes() throws Exception {
        byte[] nullBytes = null;

        catchException(checksumService).checksum(nullBytes);

        Assert.assertThat(caughtException(),
                allOf(
                        instanceOf(IllegalArgumentException.class)
                )
        );
    }

    @Test
    public void shouldReturnChecksumForEmptyString() throws Exception {
        String checksum = checksumService.checksum("");
        assertThat(checksum, is(equalTo("a69f73cca23a9ac5c8b567dc185a756e97c982164fe25859e0d1dcc1475c80a615b2123af1f5f94c11e3e9402c3ac558f500199d95b6d3e301758586281dcd26")));
    }

    @Test
    public void shouldReturnChecksumForABC() throws Exception {
        String checksum = checksumService.checksum("ABC");
        assertThat(checksum, is(equalTo("077aa33882b1aaf06da41c7ed3b6a40d7128dee23505ca2689c47637111c4701645fabc5ee1b9dcd039231d2d086bff9819ce2da8647432a73966494dd1a77ad")));
    }

    @Test
    public void shouldReturnChecksumForABCasBytes() throws Exception {
        String checksum = checksumService.checksum(new byte[]{65, 66, 67});
        assertThat(checksum, is(equalTo("077aa33882b1aaf06da41c7ed3b6a40d7128dee23505ca2689c47637111c4701645fabc5ee1b9dcd039231d2d086bff9819ce2da8647432a73966494dd1a77ad")));
    }

    @Test
    public void shouldReturnChecksumForJson() throws Exception {
        String checksum = checksumService.checksum("{\"property\":\"value\"}");
        assertThat(checksum, is(equalTo("118dd3237b94e86dc939bf28cdfbb24265101e754178c29b80f46efcaedc84aa5c2c9711a5b6438389c87f9f0ba0a2f105ec272412b69bcbeeba8eb96cfb7771")));
    }
}
