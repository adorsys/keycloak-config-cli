package de.adorsys.keycloak.config.utils;

import java.io.IOException;
import java.util.Objects;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

public class PartialUpdaterTest {

	@Test
	public void testDeepEquals() {
		SampleAddress sampleAddress = new SampleAddress("street 2", "new york", "23456", "usa", true, null);
		SamplePerson samplePerson = new SamplePerson("Francis", "2435234", sampleAddress, null, false);
		SampleCompany sampleCompany = new SampleCompany("adorsys", "3563456", samplePerson, null, false, true);
		
		// cheange one property
		SampleAddress sampleAddress2 = new SampleAddress("street 2", "new york", "23456", "usa", true, null);
		SamplePerson samplePerson2 = new SamplePerson("Francis", "2435234", sampleAddress2, null, false);
		SampleCompany sampleCompany2 = new SampleCompany("adorsys", "3563456", samplePerson2, null, false, true);
		
		Assert.assertTrue(Objects.deepEquals(sampleCompany2, sampleCompany));
		
		sampleAddress2.setKnown(sampleAddress2.getKnown()==null?false:!sampleAddress2.getKnown());
				
		Assert.assertFalse(Objects.deepEquals(sampleCompany2, sampleCompany));
				
	}

	@Test
	public void testFullCopy() throws IOException {
		SampleAddress sampleAddress = new SampleAddress("street 2", "new york", "23456", "usa", true, null);
		SamplePerson samplePerson = new SamplePerson("Francis", "2435234", sampleAddress, null, false);
		SampleCompany sampleCompany = new SampleCompany("adorsys", "3563456", samplePerson, sampleAddress, false, true);
		
		SampleCompany sampleCompany2 = PartialUpdater.deepCloneObject(sampleCompany);

		Assert.assertFalse(sampleCompany==sampleCompany2);
		Assert.assertTrue(Objects.deepEquals(sampleCompany2, sampleCompany));
	}



	@Test
	public void testPartialCopy() throws IOException {
		SampleAddress sampleAddress = new SampleAddress("street 2", "new york", "23456", "usa", true, null);
		SamplePerson samplePerson = new SamplePerson("Francis", "2435234", sampleAddress, null, false);
		SampleCompany sampleCompany = new SampleCompany("adorsys", "3563456", samplePerson, sampleAddress, false, true);
		
		SampleCompany sampleCompany2 = PartialUpdater.deepCloneObject(sampleCompany);
		
		Assume.assumeFalse(sampleCompany==sampleCompany2);
		Assume.assumeTrue(Objects.deepEquals(sampleCompany2, sampleCompany));
				
		SampleCompany sampleCompany3 = new SampleCompany("teambank", null, null, null, null, null);
		
		SampleCompany sampleCompany4 =  PartialUpdater.deepPatchObject(sampleCompany3, sampleCompany);
		
		Assert.assertTrue("teambank".equals(sampleCompany4.getName()));
		Assume.assumeFalse(Objects.deepEquals(sampleCompany2, sampleCompany4));
		
		sampleCompany2.setName("teambank");
		Assert.assertTrue(Objects.deepEquals(sampleCompany2, sampleCompany4));
	}

	@Test
	public void testPartialCopy2() throws IOException {
		SampleAddress sampleAddress = new SampleAddress("street 2", "new york", "23456", "usa", true, null);
		SamplePerson samplePerson = new SamplePerson("Francis", "2435234", sampleAddress, null, false);
		SampleCompany sampleCompany = new SampleCompany("adorsys", "3563456", samplePerson, sampleAddress, false, true);
		
		SampleCompany sampleCompany2 = PartialUpdater.deepCloneObject(sampleCompany);
		
		Assume.assumeFalse(sampleCompany==sampleCompany2);
		Assume.assumeTrue(Objects.deepEquals(sampleCompany2, sampleCompany));
				

		SampleAddress sampleAddress2 = new SampleAddress();
		sampleAddress2.setCountry("cameroon");
		sampleAddress2.setKnown(false);
		SampleCompany sampleCompany3 = new SampleCompany();
		sampleCompany3.setAddress(sampleAddress2);
		
		SampleCompany sampleCompany4 =  PartialUpdater.deepPatchObject(sampleCompany3, sampleCompany);
		Assume.assumeFalse(Objects.deepEquals(sampleCompany4, sampleCompany));
		Assume.assumeFalse(Objects.deepEquals(sampleCompany2, sampleCompany4));
		Assume.assumeTrue(Objects.deepEquals(sampleCompany2, sampleCompany));

		Assume.assumeFalse(Objects.deepEquals(sampleCompany3.getAddress(), sampleCompany4.getAddress()));
		Assume.assumeTrue(Objects.deepEquals(sampleCompany3.getKnown(), sampleCompany4.getKnown()));
		
		sampleCompany2.getAddress().setCountry("cameroon");
		sampleCompany2.getAddress().setKnown(false);
		Assert.assertTrue(Objects.deepEquals(sampleCompany2, sampleCompany4));
	}
}
