package uk.org.taverna.astro.vorepo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.List;

import javax.xml.ws.BindingProvider;

import net.ivoa.xml.conesearch.v1.ConeSearch;
import net.ivoa.xml.voresource.v1.Capability;
import net.ivoa.xml.voresource.v1.Resource;
import net.ivoa.xml.voresource.v1.Service;

import org.junit.Ignore;
import org.junit.Test;

import uk.org.taverna.astro.wsdl.registrysearch.RegistrySearchPortType;

/*
 To debug SOAP messages, set system properties:

 -Dcom.sun.xml.ws.transport.http.client.HttpTransportPipe.dump=true
 -Dcom.sun.xml.internal.ws.transport.http.client.HttpTransportPipe.dump=true
 */
public class TestVORepository {

	@Test
	public void keywordSearch() throws Exception {
		VORepository repo = new VORepository();
		List<Resource> resources = repo
				.keywordSearch("ivo://svo.amiga.iaa.es/amiga");
		assertFalse(resources.isEmpty());
		Resource someResource = resources.get(0);
		assertTrue(someResource.getIdentifier().startsWith("ivo://"));

	}

	@Test
	public void coneSearch() throws Exception {
		VORepository repo = new VORepository();
		List<Service> resources = repo.resourceSearch(
				ConeSearch.class, "amiga");
		assertFalse(resources.isEmpty());	
		Service s = resources.get(0);
		boolean foundCapability = false;
		for (Capability c: s.getCapability()) {
			if (c instanceof ConeSearch) {
				ConeSearch coneSearch = (ConeSearch) c;
				foundCapability = true;
				//System.out.println(coneSearch.getInterface().get(0).getAccessURL().get(0).getValue());				
			}
		}
		assertTrue("Could not find any ConeSearch", foundCapability);
	}

	@Test
	public void defaultRepo() throws Exception {
		VORepository repo = new VORepository();
		assertEquals("http://registry.euro-vo.org/services/RegistrySearch",
				repo.getEndpoint().toASCIIString());
	}

	@Test
	public void status() throws Exception {
		assertEquals(VORepository.Status.OK, new VORepository().getStatus());
	}

	@Test
	public void portCached() throws Exception {
		VORepository voRepository = new VORepository();
		RegistrySearchPortType port = voRepository.getPort();
		assertSame(port, voRepository.getPort());
	}

	@Test
	public void statusWrongEndpoint() throws Exception {
		assertEquals(
				VORepository.Status.CONNECTION_ERROR,
				new VORepository(
						URI.create("http://registry.euro-vo.org/services/RegistryHarvest"))
						.getStatus());
	}

	@Test
	public void status404() throws Exception {
		assertEquals(VORepository.Status.CONNECTION_ERROR,
				new VORepository(URI.create("http://example.com/404"))
						.getStatus());
	}

	@Test
	public void endPointChanged() {
		VORepository voRepository = new VORepository();
		RegistrySearchPortType firstPort = voRepository.getPort();
		assertEquals(
				"http://registry.euro-vo.org/services/RegistrySearch",
				((BindingProvider) firstPort).getRequestContext().get(
						BindingProvider.ENDPOINT_ADDRESS_PROPERTY));
		voRepository.setEndpoint(URI.create("http://example.com/404"));
		RegistrySearchPortType secondPort = voRepository.getPort();
		assertNotSame(firstPort, secondPort);
		assertEquals(
				"http://example.com/404",
				((BindingProvider) secondPort).getRequestContext().get(
						BindingProvider.ENDPOINT_ADDRESS_PROPERTY));

	}

	@Test
	public void portUncached() throws Exception {
		VORepository voRepository = new VORepository();
		RegistrySearchPortType port = voRepository.getPort();
		voRepository.setEndpoint(URI.create("http://example.com/404"));
		assertNotSame(port, voRepository.getPort());
	}

	// Ignored as this adds 21s
	@Ignore
	@Test
	public void statusTimeout() throws Exception {
		assertEquals(VORepository.Status.CONNECTION_ERROR,
				new VORepository(URI.create("http://example.com:12345/"))
						.getStatus());
	}

}
