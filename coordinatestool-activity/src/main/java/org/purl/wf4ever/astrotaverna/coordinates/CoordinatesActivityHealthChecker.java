package org.purl.wf4ever.astrotaverna.coordinates;

import java.util.ArrayList;
import java.util.List;

/**
 * coordinates health checker
 * 
 */
public class CoordinatesActivityHealthChecker implements
		HealthChecker<CoordinatesActivity> {

	public boolean canVisit(Object o) {
		// Return True if we can visit the object. We could do
		// deeper (but not time consuming) checks here, for instance
		// if the health checker only deals with coordinatesActivity where
		// a certain configuration option is enabled.
		return o instanceof CoordinatesActivity;
	}

	public boolean isTimeConsuming() {
		// Return true if the health checker does a network lookup
		// or similar time consuming checks, in which case
		// it would only be performed when using File->Validate workflow
		// or File->Run.
		return false;
	}

	public VisitReport visit(CoordinatesActivity activity, List<Object> ancestry) {
		CoordinatesActivityConfigurationBean config = activity.getConfiguration();

		// We'll build a list of subreports
		List<VisitReport> subReports = new ArrayList<VisitReport>();

		if (!config.getExampleUri().isAbsolute()) {
			// Report Severe problems we know won't work
			VisitReport report = new VisitReport(HealthCheck.getInstance(),
					activity, "Example URI must be absolute", HealthCheck.INVALID_URL,
					Status.SEVERE);
			subReports.add(report);
		}

		if (config.getExampleString().equals("")) {
			// Warning on possible problems
			subReports.add(new VisitReport(HealthCheck.getInstance(), activity,
					"Example string empty", HealthCheck.NO_CONFIGURATION,
					Status.WARNING));
		}

		// The default explanation here will be used if the subreports list is
		// empty
		return new VisitReport(HealthCheck.getInstance(), activity,
				"coordinates service OK", HealthCheck.NO_PROBLEM, subReports);
	}

}
