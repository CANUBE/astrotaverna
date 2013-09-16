package org.purl.wf4ever.astrotaverna.coordinates.ui.serviceprovider;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;

//public class coordinatesServiceProvider implements ServiceDescriptionProvider {
public class CoordinatesServiceProvider implements
		ServiceDescriptionProvider {

	public CoordinatesServiceProvider() {	
	}

	private static final URI providerId = URI
			.create("http://example.com/2011/service-provider/coordinatestool");

	/**
	 * Do the actual search for services. Return using the callBack parameter.
	 */
	@SuppressWarnings("unchecked")
	public void findServiceDescriptionsAsync(
			FindServiceDescriptionsCallBack callBack) {
		// Use callback.status() for long-running searches
		// callBack.status("Resolving example services");

		List<ServiceDescription> results = new ArrayList<ServiceDescription>();

		// FIXME: Implement the actual service search/lookup instead
		// of dummy for-loop
		// for (int i = 1; i <= 5; i++) {
		CoordinatesServiceDesc service = new CoordinatesServiceDesc();
		// Populate the service description bean
		service.setExampleString("Equatorials2Galactic");
		// service.setExampleUri(URI.create("http://localhost:8192/service"));

		// Optional: set description
		// service.setDescription("Service example number " + i);
		results.add(service);

		CoordinateDegreeServiceDesc service2 = new CoordinateDegreeServiceDesc();
		service2.setExampleString("hms2degrees");
		results.add(service2);
		// }
		// for (int i = 1; i <= getConfiguration().getNumberOfServices(); i++) {
		// coordinatesServiceDesc service = new coordinatesServiceDesc();
		// service.setExampleString("Example " + i);
		// service.setExampleUri(getConfiguration().getUri());
		// results.add(service);
		// }

		// partialResults() can also be called several times from inside
		// for-loop if the full search takes a long time
		callBack.partialResults(results);

		// No more results will be coming
		callBack.finished();
	}

	/**
	 * Icon for service provider
	 */
	public Icon getIcon() {
		return CoordinatesServiceIcon.getIcon();
	}

	/**
	 * Name of service provider, appears in right click for 'Remove service
	 * provider'
	 */
	public String getName() {
		return "Coordinate tools";
	}

	@Override
	public String toString() {
		return getName();
	}

	public String getId() {
		return providerId.toASCIIString();
	}

}
