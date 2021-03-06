package com.tenable.io.api;


import com.tenable.io.api.models.SeverityLevel;
import com.tenable.io.api.plugins.models.PluginDetail;
import com.tenable.io.api.plugins.models.PluginFamily;
import com.tenable.io.api.plugins.models.PluginFamilyDetail;
import com.tenable.io.api.scans.models.ScanVulnerability;

import com.tenable.io.api.workbenches.WorkbenchNessusFileParser;
import com.tenable.io.api.workbenches.models.*;
import org.junit.Test;

import java.io.File;
import java.util.*;

import static org.junit.Assert.*;


/**
 * Copyright (c) 2017 Tenable Network Security, Inc.
 */
public class WorkbenchesApiClientTest extends TestBase {

    @Test
    public void testVulnerabilities() throws Exception {
        TenableIoClient apiClient = new TenableIoClient();

        List<ScanVulnerability> result = apiClient.getWorkbenchesApi().vulnerabilities( new ExtendedFilteringOptions() );
        if(result != null && result.size() > 0) {
            assertNotNull(result.get(0));
            assertTrue(result.get(0).getPluginId() > 0);
            assertTrue(result.get(0).getSeverity() > 0);
        }

        result = apiClient.getWorkbenchesApi().vulnerabilities( new ExtendedFilteringOptions().withSeverity( SeverityLevel.CRITICAL ) );
        if(result != null && result.size() > 0) {
            assertTrue(result.size() > 0);
            assertNotNull(result.get(0));
            assertTrue(result.get(0).getPluginId() > 0);
            assertTrue(result.get(0).getSeverity() == 4);

            WbVulnerabilityInfo info = apiClient.getWorkbenchesApi().vulnerabilityInfo(result.get(0).getPluginId(), new FilteringOptions());
            assertNotNull(info);
            assertTrue(info.getSeverity() == 4);
            assertNotNull(info.getDescription());
            assertNotNull(info.getSolution());
            assertNotNull(info.getSynopsis());
            assertNotNull(info.getPluginDetails());
            assertNotNull(info.getDiscovery());

            List<WbVulnerabilityOutputResult> items = apiClient.getWorkbenchesApi().vulnerabilityOutput( result.get(0).getPluginId(), new FilteringOptions());
            assertNotNull( items );
            assertTrue( items.size() > 0 );
            assertNotNull( items.get(0) );
            assertNotNull( items.get(0).getPluginOutput() );
            assertNotNull( items.get(0).getStates() );

        }

    }

    @Test
    public void testAssets() throws Exception {
        TenableIoClient apiClient = new TenableIoClient();

        List<WbVulnerabilityAsset> assets = apiClient.getWorkbenchesApi().assets(new FilteringOptions());
        if(assets != null && assets.size() > 0) {
            assertNotNull(assets.get(0));
            assertNotNull(assets.get(0).getId());
            assertNotNull(assets.get(0).getLastSeen());

            WbAssetInfo assetInfo = apiClient.getWorkbenchesApi().assetInfo(assets.get(0).getId(), new FilteringOptions());
            assertNotNull(assetInfo);

            List<ScanVulnerability> vulnerabilities = apiClient.getWorkbenchesApi().assetVulnerabilities(
                    assets.get(0).getId(), new FilteringOptions());
            assertNotNull(vulnerabilities);
            assertTrue( vulnerabilities.size() > 0 );
            assertTrue(vulnerabilities.get(0).getCount() > 0);
            assertNotNull(vulnerabilities.get(0).getPluginId());
            assertTrue(vulnerabilities.get(0).getVulnerabilityState().equals("Active"));

            WbVulnerabilityInfo info = apiClient.getWorkbenchesApi().vulnerabilityInfo(assets.get(0).getId(),
                    vulnerabilities.get(0).getPluginId(), new FilteringOptions());
            assertNotNull(info);
            assertNotNull(info.getDescription());
            assertNotNull(info.getSynopsis());

            List<WbVulnerabilityOutputResult> assetVulnerabilityOutput = apiClient.getWorkbenchesApi()
                    .assetVulnerabilityOutput(assets.get(0).getId(), vulnerabilities.get(0).getPluginId(),
                            new FilteringOptions());
            assertNotNull(assetVulnerabilityOutput);
            assertTrue(assetVulnerabilityOutput.size() > 0);
            assertNotNull(assetVulnerabilityOutput.get(0).getPluginOutput());
            assertNotNull(assetVulnerabilityOutput.get(0).getStates());
        }

        List<WbVulnerabilityAsset> assetVuln = apiClient.getWorkbenchesApi().assetsVulnerabilities(new FilteringOptions());
        if(assetVuln != null && assetVuln.size() > 0) {
            assertNotNull(assetVuln.get(0));
            assertNotNull(assetVuln.get(0).getId());
            assertNotNull(assetVuln.get(0).getSeverities());
            assertTrue(assetVuln.get(0).getSeverities().size() > 0);
        }



    }

    @Test
    public void testWorkbenchExport() throws Exception {

        TenableIoClient apiClient = new TenableIoClient();

        File destinationFile = new File("src/test/resources/workbenchTest.nessus");

        List<Filter> filters = new ArrayList<Filter>();
        Filter severity = new Filter();
        severity.setFilter("severity");
        severity.setQuality( FilterOperator.GREATER_THAN );
        severity.setValue("0");
        filters.add(severity);

        int fileId = apiClient.getWorkbenchesApi().exportRequest( new ExportOptions().withFormat( FileFormat.NESSUS )
                .withReport( ReportType.VULNERABILITIES )
                .withChapter( "vuln_by_plugin;vuln_by_asset;vuln_hosts_summary" )
                .withFilters(filters));


        while( !"ready".equals( apiClient.getWorkbenchesApi().exportStatus( fileId ) ) ) {
            try {
                Thread.sleep( 5000 );
            } catch( InterruptedException e ) {}
        }

        apiClient.getWorkbenchesApi().exportDownload( fileId, destinationFile );

        assertTrue( destinationFile.exists() );

        destinationFile.delete();
    }


    private FilteringOptions GetFilteringOptions() throws Exception {
        FilteringOptions options = new FilteringOptions();
        options.setDateRange( 3 );
        List<Filter> filters = new ArrayList<>();
        Filter filter1 = new Filter();
        filter1.setFilter( "host.hostname" );
        filter1.setQuality( FilterOperator.CONTAINS );
        filter1.setValue( getTestDomain() );
        filters.add( filter1 );
        Filter filter2 = new Filter();
        filter2.setFilter( "host.port" );
        filter2.setQuality( FilterOperator.CONTAINS );
        filter2.setValue( "80" );
        filters.add( filter2 );
        options.setFilters( filters );
        options.setSearchType( FilterSearchType.AND );
        return options;
    }

    private ExtendedFilteringOptions GetExtendedFilteringOptions() throws Exception {
        ExtendedFilteringOptions options = new ExtendedFilteringOptions();
        options.setAge( 10 );
        options.setAuthenticated( true );
        options.setDateRange( 3 );
        options.setExploitable( true );
        List<Filter> filters = new ArrayList<>();
        Filter filter1 = new Filter();
        filter1.setFilter( "host.hostname" );
        filter1.setQuality( FilterOperator.CONTAINS );
        filter1.setValue( getTestDomain() );
        filters.add( filter1 );
        Filter filter2 = new Filter();
        filter2.setFilter( "host.port" );
        filter2.setQuality( FilterOperator.CONTAINS );
        filter2.setValue( "80" );
        filters.add( filter2 );
        options.setFilters( filters );
        options.setSearchType( FilterSearchType.AND );
        options.setResolvable( false );
        options.setSeverity( SeverityLevel.HIGH );
        return options;
    }

    private int getPluginId() throws Exception {
        TenableIoClient apiClient = new TenableIoClient();
        List<PluginFamily> pluginFamilies = apiClient.getPluginsApi().families();
        PluginFamilyDetail familyDetails = apiClient.getPluginsApi().familyDetails( pluginFamilies.get( 0 ).getId() );
        PluginDetail pluginDetails = apiClient.getPluginsApi().pluginDetails( familyDetails.getPlugins().get( 0 ).getId() );
        return pluginDetails.getId();
    }

}
