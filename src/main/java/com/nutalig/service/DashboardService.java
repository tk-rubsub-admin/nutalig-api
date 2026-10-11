package com.nutalig.service;

import com.nutalig.constant.RfqStatus;
import com.nutalig.constant.ActivityEntityType;
import com.nutalig.constant.ApprovalRequestType;
import com.nutalig.config.AppProperties;
import com.nutalig.dto.*;
import com.nutalig.entity.*;
import com.nutalig.repository.QuotationRepository;
import com.nutalig.repository.RequestPriceHeaderRepository;
import com.nutalig.repository.RfqStatusTimelineRepository;
import com.nutalig.repository.ApprovalRequestRepository;
import com.nutalig.utils.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.nutalig.repository.specification.RequestPriceHeaderSpecification.procurementIdEqual;
import static com.nutalig.repository.specification.RequestPriceHeaderSpecification.requestedDateBetween;
import static com.nutalig.repository.specification.RequestPriceHeaderSpecification.salesIdEqual;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ApprovalRequestRepository approvalRequestRepository;

    private static final DateTimeFormatter REQUEST_DATE_FORMAT = DateTimeFormatter.ofPattern("MM/yyyy");
    // Locked tokens from NUTALIG CI Active Source Rev.03, page 7.
    private static final String BRAND_GREEN = "#1F3F37";
    private static final String BRAND_SAND = "#D4C5BA";
    private static final String BRAND_TERRACOTTA = "#925944";
    private static final String BRAND_CHARCOAL = "#30302F";
    // Additional categories use brand-derived tints, not additional official brand colors.
    private static final List<String> DISTRIBUTION_PALETTE = List.of(
            BRAND_GREEN, BRAND_TERRACOTTA, BRAND_SAND, BRAND_CHARCOAL,
            "#6B7E77", "#B58F80", "#ABA097", "#ABB4AE", "#D4BDB2", "#94928E"
    );
    // Any selected range of up to three consecutive months has three distinct brand colors.
    private static final List<String> MONTH_PALETTE = List.of(
            BRAND_GREEN, BRAND_TERRACOTTA, BRAND_SAND,
            BRAND_GREEN, BRAND_TERRACOTTA, BRAND_SAND,
            BRAND_GREEN, BRAND_TERRACOTTA, BRAND_SAND,
            BRAND_GREEN, BRAND_TERRACOTTA, BRAND_SAND
    );
    private static final List<RfqStatus> IN_PROGRESS_QUEUE_STATUSES = List.of(RfqStatus.IN_PROGRESS, RfqStatus.REQUESTED_INFO);
    private static final Set<String> RFQ_CREATE_ROLES = Set.of("SUPER_ADMIN", "ADMIN", "SALES", "SALES_MANAGER", "SALES_ADMIN");
    private static final List<String> SALES_VISIBLE_TO = List.of("SUPER_ADMIN", "ADMIN", "SALES", "SALES_MANAGER", "SALES_ADMIN");
    private static final List<String> PROCUREMENT_VISIBLE_TO = List.of("SUPER_ADMIN", "ADMIN", "PROCUREMENT", "PROCUREMENT_ADMIN");
    private static final List<String> ALL_RFQ_VISIBLE_TO = List.of("SUPER_ADMIN", "ADMIN", "SALES", "SALES_MANAGER", "SALES_ADMIN", "PROCUREMENT", "PROCUREMENT_ADMIN");
    private static final String RFQ_MANAGEMENT_PATH = "/rfq-management";
    private static final String PRICE_INQUIRY_MANAGEMENT_PATH = "/price-inquiry-management";
    private static final String RFQ_CREATE_PATH = "/rfq-create";

    private final RequestPriceHeaderRepository requestPriceHeaderRepository;
    private final QuotationRepository quotationRepository;
    private final RfqStatusTimelineRepository rfqStatusTimelineRepository;
    private final BusinessDurationService businessDurationService;
    private final AppProperties appProperties;

    @Transactional(readOnly = true)
    public DashboardDataDto getDashboard(
            String dateFrom,
            String dateTo,
            String salesId,
            String procurementId
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDto user = authentication != null && authentication.getPrincipal() instanceof UserDto userDto
                ? userDto
                : null;
        String roleCode = user != null && user.getRole() != null ? user.getRole().getRoleCode() : null;
        String employeeId = user != null ? StringUtils.trimToNull(user.getEmployeeId()) : null;
        String filterSalesId = StringUtils.trimToNull(salesId);
        String filterProcurementId = StringUtils.trimToNull(procurementId);

        LocalDate endDate = parseLocalDateOrDefault(dateTo, LocalDate.now(DateUtil.getTimeZone()));
        LocalDate startDate = parseLocalDateOrDefault(dateFrom, endDate.minusDays(6));
        if (startDate.isAfter(endDate)) {
            LocalDate swap = startDate;
            startDate = endDate;
            endDate = swap;
        }

        Specification<RfqHeaderEntity> specification = Specification.where(requestedDateBetween(startDate, endDate));
//        if (StringUtils.equals(roleCode, "SALES") && employeeId != null) {
//            specification = specification.and(salesIdEqual(employeeId));
//        } else if (StringUtils.equals(roleCode, "PROCUREMENT") && employeeId != null) {
//            specification = specification.and(procurementIdEqual(employeeId));
//        }
        if (filterSalesId != null) {
            specification = specification.and(salesIdEqual(filterSalesId));
        }
        if (filterProcurementId != null) {
            specification = specification.and(procurementIdEqual(filterProcurementId));
        }

        Map<String, String> selectedFilters = new LinkedHashMap<>();
        if (filterSalesId != null) {
            selectedFilters.put("salesId", filterSalesId);
        }
        if (filterProcurementId != null) {
            selectedFilters.put("procurementId", filterProcurementId);
        }

        List<RfqHeaderEntity> rfqs = requestPriceHeaderRepository.findAll(
                specification,
                Sort.by(Sort.Direction.DESC, "requestedDate")
        );
        Set<String> rfqIdsWithQuotation = quotationRepository.findAllByRfqIdIn(
                rfqs.stream().map(RfqHeaderEntity::getId).toList()
        ).stream()
                .map(QuotationEntity::getRfqId)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());

        List<LocalDate> months = enumerateMonths(startDate, endDate);
        DashboardDataDto dashboard = new DashboardDataDto();
        dashboard.setRange("CUSTOM");
        dashboard.setDateFrom(startDate.toString());
        dashboard.setDateTo(endDate.toString());
        dashboard.setGeneratedAt(ZonedDateTime.now(DateUtil.getTimeZone()));
        dashboard.setSource("api");
        dashboard.setMetrics(buildMetrics(rfqs, startDate, endDate, selectedFilters));
        dashboard.setTrendCharts(buildTrendCharts(rfqs, startDate, endDate, roleCode));
        dashboard.setAcceptWorkDurationChart(buildAcceptWorkDurationChart(rfqs, roleCode, months));
        dashboard.setSupplierQuoteDurationChart(buildSupplierQuoteDurationChart(rfqs, roleCode, months));
        dashboard.setProcurementKpiChart(buildProcurementKpiChart(rfqs, months));
        dashboard.setSalesCountChart(buildSalesCountChart(rfqs, roleCode, months));
        dashboard.setCustomerTypeCountChart(buildCustomerTypeCountChart(rfqs, roleCode, months));
        dashboard.setDistributionCharts(buildDistributionCharts(rfqs, roleCode));
        dashboard.setWorkQueues(buildWorkQueues(rfqs, startDate, endDate, selectedFilters, rfqIdsWithQuotation));
        dashboard.setQuickLinks(buildQuickLinks(roleCode, startDate, endDate, selectedFilters));
        return dashboard;
    }

    private List<DashboardMetricDto> buildMetrics(
            List<RfqHeaderEntity> rfqs,
            LocalDate startDate,
            LocalDate endDate,
            Map<String, String> selectedFilters
    ) {

        return List.of(
                metric(
                        "rfq-total",
                        "dashboard.rfq.metrics.total.title",
                        rfqs.size(),
                        "dashboard.rfq.metrics.total.subtitle",
                        null,
                        "info",
                        buildRfqManagementHref(startDate, endDate, selectedFilters, Map.of()),
                        ALL_RFQ_VISIBLE_TO
                ),
                metric(
                        "rfq-new",
                        "dashboard.rfq.metrics.new.title",
                        countByStatus(rfqs, RfqStatus.NEW),
                        "dashboard.rfq.metrics.new.subtitle",
                        null,
                        "warning",
                        buildRfqManagementHref(startDate, endDate, selectedFilters, Map.of("statuses", RfqStatus.NEW.name(), "isAccept", "false")),
                        ALL_RFQ_VISIBLE_TO
                ),
                metric(
                        "rfq-in-progress",
                        "dashboard.rfq.metrics.inProgress.title",
                        countByStatus(rfqs, RfqStatus.IN_PROGRESS),
                        "dashboard.rfq.metrics.inProgress.subtitle",
                        null,
                        "info",
                        buildPriceInquiryManagementHref(startDate, endDate, selectedFilters, Map.of("statuses", RfqStatus.IN_PROGRESS.name())),
                        ALL_RFQ_VISIBLE_TO
                ),
                metric(
                        "rfq-supplier-quoted",
                        "dashboard.rfq.metrics.supplierQuoted.title",
                        countByStatus(rfqs, RfqStatus.SUPPLIER_QUOTED),
                        "dashboard.rfq.metrics.supplierQuoted.subtitle",
                        null,
                        "neutral",
                        buildRfqManagementHref(startDate, endDate, selectedFilters, Map.of("statuses", RfqStatus.SUPPLIER_QUOTED.name())),
                        ALL_RFQ_VISIBLE_TO
                ),
                metric(
                        "rfq-quoted",
                        "dashboard.rfq.metrics.quoted.title",
                        countByStatus(rfqs, RfqStatus.QUOTED),
                        "dashboard.rfq.metrics.quoted.subtitle",
                        null,
                        "neutral",
                        buildRfqManagementHref(startDate, endDate, selectedFilters, Map.of("statuses", RfqStatus.QUOTED.name())),
                        ALL_RFQ_VISIBLE_TO
                ),
                metric(
                        "rfq-special-price",
                        "dashboard.rfq.metrics.specialPrice.title",
                        countByStatus(rfqs, RfqStatus.SPECIAL_PRICE_REVIEW),
                        "dashboard.rfq.metrics.specialPrice.subtitle",
                        null,
                        "warning",
                        buildPriceInquiryManagementHref(startDate, endDate, selectedFilters, Map.of("statuses", RfqStatus.SPECIAL_PRICE_REVIEW.name())),
                        ALL_RFQ_VISIBLE_TO
                ),
                metric(
                        "rfq-completed",
                        "dashboard.rfq.metrics.completed.title",
                        countByStatus(rfqs, RfqStatus.COMPLETED),
                        "dashboard.rfq.metrics.completed.subtitle",
                        null,
                        "success",
                        buildPriceInquiryManagementHref(startDate, endDate, selectedFilters, Map.of("statuses", RfqStatus.COMPLETED.name())),
                        ALL_RFQ_VISIBLE_TO
                ),
                metric(
                        "rfq-requested-info",
                        "dashboard.rfq.metrics.requestedInfo.title",
                        countByStatus(rfqs, RfqStatus.REQUESTED_INFO),
                        "dashboard.rfq.metrics.requestedInfo.subtitle",
                        null,
                        "warning",
                        buildPriceInquiryManagementHref(startDate, endDate, selectedFilters, Map.of("statuses", RfqStatus.REQUESTED_INFO.name())),
                        ALL_RFQ_VISIBLE_TO
                ),
                metric(
                        "rfq-closed",
                        "dashboard.rfq.metrics.closed.title",
                        countByStatus(rfqs, RfqStatus.CLOSED),
                        "dashboard.rfq.metrics.closed.subtitle",
                        null,
                        "neutral",
                        buildPriceInquiryManagementHref(startDate, endDate, selectedFilters, Map.of("statuses", RfqStatus.CLOSED.name())),
                        ALL_RFQ_VISIBLE_TO
                )
        );
    }

    private List<DashboardTrendChartDto> buildTrendCharts(
            List<RfqHeaderEntity> rfqs,
            LocalDate startDate,
            LocalDate endDate,
            String roleCode
    ) {
        List<LocalDate> dates = enumerateMonths(startDate, endDate);
        Map<LocalDate, List<RfqHeaderEntity>> rfqByDate = rfqs.stream()
                .filter(rfq -> rfq.getRequestedDate() != null)
                .collect(Collectors.groupingBy(
                        this::rfqMonth,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        Set<String> urgentRfqIds = approvalRequestRepository
                .findAllByEntityTypeAndRequestType(ActivityEntityType.RFQ, ApprovalRequestType.URGENT_RFQ)
                .stream()
                .map(approval -> approval.getReferenceId())
                .collect(Collectors.toSet());

        DashboardTrendChartDto volumeChart = new DashboardTrendChartDto();
        volumeChart.setId("rfq-volume");
        volumeChart.setTitle("dashboard.rfq.charts.volume.title");
        volumeChart.setSubtitle("dashboard.rfq.charts.volume.subtitle");
        volumeChart.setUnit("COUNT");
        volumeChart.setLabels(dates.stream().map(date -> date.format(REQUEST_DATE_FORMAT)).toList());
        volumeChart.setSeries(List.of(
                series("งานปกติ", BRAND_GREEN, dates, rfqByDate, items -> (double) items.stream().filter(item -> !urgentRfqIds.contains(item.getId())).count()),
                series("งานเร่งด่วน", BRAND_TERRACOTTA, dates, rfqByDate, items -> (double) items.stream().filter(item -> urgentRfqIds.contains(item.getId())).count())
        ));
        volumeChart.setVisibleTo(ALL_RFQ_VISIBLE_TO);

        DashboardTrendChartDto stageChart = new DashboardTrendChartDto();
        stageChart.setId("rfq-stage-progress");
        stageChart.setTitle(StringUtils.equals(roleCode, "PROCUREMENT")
                ? "dashboard.rfq.charts.procurementFlow.title"
                : "dashboard.rfq.charts.salesFlow.title");
        stageChart.setSubtitle(StringUtils.equals(roleCode, "PROCUREMENT")
                ? "dashboard.rfq.charts.procurementFlow.subtitle"
                : "dashboard.rfq.charts.salesFlow.subtitle");
        stageChart.setUnit("COUNT");
        stageChart.setLabels(dates.stream().map(date -> date.format(REQUEST_DATE_FORMAT)).toList());
        stageChart.setSeries(List.of(
                series("ใหม่", BRAND_SAND, dates, rfqByDate, items -> (double) items.stream().filter(item -> item.getStatus() == RfqStatus.NEW).count()),
                series("กำลังดำเนินการ", BRAND_GREEN, dates, rfqByDate, items -> (double) items.stream().filter(item -> item.getStatus() == RfqStatus.IN_PROGRESS).count()),
                series("ซัพตอบแล้ว", "#6B7E77", dates, rfqByDate, items -> (double) items.stream().filter(item -> item.getStatus() == RfqStatus.SUPPLIER_QUOTED).count()),
                series("เสนอราคาแล้ว", BRAND_TERRACOTTA, dates, rfqByDate, items -> (double) items.stream().filter(item -> item.getStatus() == RfqStatus.QUOTED).count())
        ));
        stageChart.setVisibleTo(StringUtils.equals(roleCode, "PROCUREMENT") ? PROCUREMENT_VISIBLE_TO : SALES_VISIBLE_TO);

        return List.of(volumeChart, stageChart);
    }

    private DashboardTrendChartDto buildAcceptWorkDurationChart(List<RfqHeaderEntity> rfqs, String roleCode, List<LocalDate> months) {
        List<RfqHeaderEntity> eligibleRfqs = rfqs.stream()
                .filter(rfq -> rfq.getRequestedDate() != null)
                .toList();
        if (eligibleRfqs.isEmpty()) {
            return null;
        }

        Set<String> rfqIdsNeedingFallback = eligibleRfqs.stream()
                .filter(rfq -> rfq.getAcceptWorkDurationMinutes() == null)
                .map(RfqHeaderEntity::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, ZonedDateTime> inProgressTimelineByRfqId = buildTimelineMap(rfqIdsNeedingFallback, RfqStatus.IN_PROGRESS);

        Map<LocalDate, long[]> monthlyCounts = new LinkedHashMap<>();
        months.forEach(month -> monthlyCounts.put(month, new long[8]));
        for (RfqHeaderEntity rfq : eligibleRfqs) {
            long[] bucketCounts = monthlyCounts.get(rfqMonth(rfq));
            Long durationMinutes = resolveAcceptWorkDurationMinutes(rfq, inProgressTimelineByRfqId);
            if (durationMinutes == null) {
                bucketCounts[7]++;
                continue;
            }

            bucketCounts[getAcceptWorkDurationBucketIndex(durationMinutes)]++;
        }

        DashboardTrendChartDto chart = new DashboardTrendChartDto();
        chart.setId("rfq-accept-work-duration");
        chart.setTitle("dashboard.rfq.charts.acceptWorkDuration.title");
        chart.setSubtitle("dashboard.rfq.charts.acceptWorkDuration.subtitle");
        chart.setUnit("COUNT");
        chart.setLabels(List.of(
                "dashboard.rfq.charts.acceptWorkDuration.buckets.lt1h",
                "dashboard.rfq.charts.acceptWorkDuration.buckets.lte3h",
                "dashboard.rfq.charts.acceptWorkDuration.buckets.lte6h",
                "dashboard.rfq.charts.acceptWorkDuration.buckets.lte1d",
                "dashboard.rfq.charts.acceptWorkDuration.buckets.lte2d",
                "dashboard.rfq.charts.acceptWorkDuration.buckets.lte3d",
                "dashboard.rfq.charts.acceptWorkDuration.buckets.gt3d",
                "dashboard.rfq.charts.acceptWorkDuration.buckets.notAccepted"
        ));
        chart.setSeries(monthlyCounts.entrySet().stream()
                .map(entry -> monthSeries(entry.getKey(), Arrays.stream(entry.getValue())
                        .mapToDouble(value -> value).boxed().toList()))
                .toList());
        chart.setVisibleTo(ALL_RFQ_VISIBLE_TO);
        return chart;
    }

    private DashboardTrendChartDto buildSupplierQuoteDurationChart(List<RfqHeaderEntity> rfqs, String roleCode, List<LocalDate> months) {
        List<RfqHeaderEntity> eligibleRfqs = rfqs.stream()
                .filter(rfq -> rfq.getRequestedDate() != null)
                .toList();
        if (eligibleRfqs.isEmpty()) {
            return null;
        }

        Set<String> rfqIds = eligibleRfqs.stream()
                .map(RfqHeaderEntity::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, ZonedDateTime> inProgressTimelineByRfqId = buildTimelineMap(rfqIds, RfqStatus.IN_PROGRESS);
        Map<String, ZonedDateTime> supplierQuotedTimelineByRfqId = buildTimelineMap(rfqIds, RfqStatus.SUPPLIER_QUOTED);

        Map<LocalDate, long[]> monthlyCounts = new LinkedHashMap<>();
        months.forEach(month -> monthlyCounts.put(month, new long[8]));
        for (RfqHeaderEntity rfq : eligibleRfqs) {
            long[] bucketCounts = monthlyCounts.get(rfqMonth(rfq));
            Long durationMinutes = resolveSupplierQuoteDurationMinutes(rfq, inProgressTimelineByRfqId, supplierQuotedTimelineByRfqId);
            if (durationMinutes == null) {
                bucketCounts[7]++;
            } else {
                bucketCounts[getAcceptWorkDurationBucketIndex(durationMinutes)]++;
            }
        }

        DashboardTrendChartDto chart = new DashboardTrendChartDto();
        chart.setId("rfq-supplier-quote-duration");
        chart.setTitle("dashboard.rfq.charts.supplierQuoteDuration.title");
        chart.setSubtitle("dashboard.rfq.charts.supplierQuoteDuration.subtitle");
        chart.setUnit("COUNT");
        chart.setLabels(List.of(
                "dashboard.rfq.charts.supplierQuoteDuration.buckets.lt1h",
                "dashboard.rfq.charts.supplierQuoteDuration.buckets.lte3h",
                "dashboard.rfq.charts.supplierQuoteDuration.buckets.lte6h",
                "dashboard.rfq.charts.supplierQuoteDuration.buckets.lte1d",
                "dashboard.rfq.charts.supplierQuoteDuration.buckets.lte2d",
                "dashboard.rfq.charts.supplierQuoteDuration.buckets.lte3d",
                "dashboard.rfq.charts.supplierQuoteDuration.buckets.gt3d",
                "dashboard.rfq.charts.supplierQuoteDuration.buckets.remaining"
        ));
        chart.setSeries(monthlyCounts.entrySet().stream()
                .map(entry -> monthSeries(entry.getKey(), Arrays.stream(entry.getValue())
                        .mapToDouble(value -> value).boxed().toList()))
                .toList());
        chart.setVisibleTo(ALL_RFQ_VISIBLE_TO);
        return chart;
    }

    private DashboardTrendChartDto buildProcurementKpiChart(List<RfqHeaderEntity> rfqs, List<LocalDate> months) {
        List<RfqHeaderEntity> eligibleRfqs = rfqs.stream()
                .filter(rfq -> rfq.getRequestedDate() != null)
                .toList();
        if (eligibleRfqs.isEmpty()) {
            return null;
        }
        Set<String> rfqIds = eligibleRfqs.stream().map(RfqHeaderEntity::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, ZonedDateTime> newTimeline = buildTimelineMap(rfqIds, RfqStatus.NEW);
        Map<String, ZonedDateTime> quotedTimeline = buildTimelineMap(rfqIds, RfqStatus.SUPPLIER_QUOTED);
        Map<LocalDate, long[]> monthlyCounts = new LinkedHashMap<>();
        months.forEach(month -> monthlyCounts.put(month, new long[6]));
        Set<RfqStatus> beforeSupplierQuoted = EnumSet.of(RfqStatus.NEW, RfqStatus.IN_PROGRESS, RfqStatus.REQUESTED_INFO);
        AppProperties.RfqPendingAcceptance acceptanceConfig = appProperties.getRfqPendingAcceptance();

        for (RfqHeaderEntity rfq : eligibleRfqs) {
            long[] counts = monthlyCounts.get(rfqMonth(rfq));
            ZonedDateTime start = newTimeline.getOrDefault(rfq.getId(), rfq.getRequestedDate());
            ZonedDateTime end = quotedTimeline.get(rfq.getId());
            if (end == null) {
                counts[beforeSupplierQuoted.contains(rfq.getStatus()) ? 4 : 5]++;
            } else if (start == null || end.isBefore(start)) {
                counts[5]++;
            } else {
                long minutes = businessDurationService.calculateBusinessDurationMinutesWithCutoff(
                        start,
                        end,
                        acceptanceConfig != null ? acceptanceConfig.getCutoffTime() : null,
                        acceptanceConfig != null ? acceptanceConfig.getNextDayStartTime() : null
                );
                counts[getProcurementKpiBucketIndex(minutes)]++;
            }
        }

        DashboardTrendChartDto chart = new DashboardTrendChartDto();
        chart.setId("rfq-procurement-kpi");
        chart.setTitle("dashboard.rfq.charts.procurementKpi.title");
        chart.setUnit("COUNT");
        chart.setLabels(List.of(
                "dashboard.rfq.charts.procurementKpi.buckets.lt1d",
                "dashboard.rfq.charts.procurementKpi.buckets.lt2d",
                "dashboard.rfq.charts.procurementKpi.buckets.lt3d",
                "dashboard.rfq.charts.procurementKpi.buckets.gte3d",
                "dashboard.rfq.charts.procurementKpi.buckets.pending",
                "dashboard.rfq.charts.procurementKpi.buckets.missingData"
        ));
        chart.setSeries(monthlyCounts.entrySet().stream()
                .map(entry -> monthSeries(entry.getKey(), Arrays.stream(entry.getValue())
                        .mapToDouble(value -> value).boxed().toList()))
                .toList());
        chart.setVisibleTo(ALL_RFQ_VISIBLE_TO);
        return chart;
    }

    private int getProcurementKpiBucketIndex(long minutes) {
        if (minutes < 1_440) {
            return 0;
        }
        if (minutes < 2_880) {
            return 1;
        }
        if (minutes < 4_320) {
            return 2;
        }
        return 3;
    }

    private List<DashboardDistributionChartDto> buildDistributionCharts(List<RfqHeaderEntity> rfqs, String roleCode) {
        List<DashboardDistributionChartDto> charts = new ArrayList<>();

        charts.add(distributionChart(
                "rfq-by-type",
                "dashboard.rfq.charts.rfqType.title",
                "dashboard.rfq.charts.rfqType.subtitle",
                buildRfqTypeBreakdown(rfqs),
                ALL_RFQ_VISIBLE_TO
        ));

        charts.add(distributionChart(
                "rfq-by-family",
                "dashboard.rfq.charts.family.title",
                "dashboard.rfq.charts.family.subtitle",
                buildBreakdown(rfqs, this::getProductFamilyLabel, 6),
                ALL_RFQ_VISIBLE_TO
        ));

        charts.add(distributionChart(
                "rfq-by-subtype1",
                "dashboard.rfq.charts.subtype1.title",
                "dashboard.rfq.charts.subtype1.subtitle",
                buildBreakdown(rfqs, this::getProductSubtype1Label, 6),
                ALL_RFQ_VISIBLE_TO
        ));

        charts.add(distributionChart(
                "rfq-by-channel",
                "dashboard.rfq.charts.channel.title",
                "dashboard.rfq.charts.channel.subtitle",
                buildBreakdown(
                        rfqs,
                        this::getContactChannelLabel,
                        Integer.MAX_VALUE
                ),
                ALL_RFQ_VISIBLE_TO
        ));

        boolean procurementView = StringUtils.equals(roleCode, "PROCUREMENT");
        charts.add(distributionChart(
                procurementView ? "rfq-by-sales" : "rfq-by-procurement",
                procurementView ? "dashboard.rfq.charts.bySales.title" : "dashboard.rfq.charts.byProcurement.title",
                procurementView ? "dashboard.rfq.charts.bySales.subtitle" : "dashboard.rfq.charts.byProcurement.subtitle",
                buildBreakdown(
                        rfqs,
                        procurementView
                                ? rfq -> getEmployeeLabel(rfq.getSales())
                                : rfq -> getEmployeeLabel(rfq.getProcurement()),
                        8
                ),
                procurementView ? PROCUREMENT_VISIBLE_TO : SALES_VISIBLE_TO
        ));

        return charts;
    }

    private String getContactChannelLabel(RfqHeaderEntity rfq) {
        String channel = StringUtils.defaultIfBlank(StringUtils.trimToNull(rfq.getContactChannel()), "-");
        if (!StringUtils.equalsIgnoreCase(channel, "LINE")) {
            return channel;
        }
        EmployeeEntity sales = rfq.getSales();
        SystemConfigEntity team = sales != null ? sales.getTeam() : null;
        if (team == null || team.getId() == null) {
            return "LINE";
        }
        return switch (StringUtils.defaultString(team.getId().getCode())) {
            case "SALES_ONLINE" -> "LINE Online";
            case "SALES_OFFLINE" -> "LINE Offline";
            default -> "LINE";
        };
    }

    private List<DashboardDistributionItemDto> buildRfqTypeBreakdown(List<RfqHeaderEntity> rfqs) {
        Map<String, String> labels = new LinkedHashMap<>();
        for (RfqHeaderEntity rfq : rfqs) {
            String code = getRfqTypeCode(rfq);
            SystemConfigEntity type = rfq.getRfqType();
            labels.putIfAbsent(code, type == null ? code : StringUtils.firstNonBlank(
                    type.getNameTh(), type.getNameEn(), code));
        }
        List<DashboardDistributionItemDto> items = buildBreakdown(rfqs, this::getRfqTypeCode, Integer.MAX_VALUE);
        items.forEach(item -> item.setLabel(labels.get(item.getLabel())));
        return items;
    }

    private String getRfqTypeCode(RfqHeaderEntity rfq) {
        SystemConfigEntity type = rfq.getRfqType();
        return type != null && type.getId() != null
                ? StringUtils.defaultIfBlank(type.getId().getCode(), "dashboard.rfq.charts.rfqType.unspecified")
                : "dashboard.rfq.charts.rfqType.unspecified";
    }

    private DashboardDistributionChartDto buildSalesCountChart(List<RfqHeaderEntity> rfqs, String roleCode, List<LocalDate> months) {
        if (rfqs == null || rfqs.isEmpty()) {
            return null;
        }

        List<DashboardDistributionItemDto> items = buildBreakdown(
                rfqs,
                rfq -> getEmployeeLabel(rfq.getSales()),
                12
        );
        if (items.isEmpty()) {
            return null;
        }

        DashboardDistributionChartDto chart = new DashboardDistributionChartDto();
        chart.setId("rfq-by-sales-count");
        chart.setTitle("dashboard.rfq.charts.bySalesCount.title");
        chart.setSubtitle("dashboard.rfq.charts.bySalesCount.subtitle");
        chart.setItems(items);
        chart.setSeries(buildMonthlyBreakdown(rfqs, months, items, rfq -> getEmployeeLabel(rfq.getSales())));
        chart.setVisibleTo(ALL_RFQ_VISIBLE_TO);
        return chart;
    }

    private List<DashboardWorkQueueDto> buildWorkQueues(
            List<RfqHeaderEntity> rfqs,
            LocalDate startDate,
            LocalDate endDate,
            Map<String, String> selectedFilters,
            Set<String> rfqIdsWithQuotation
    ) {
        return List.of(
                queue(
                        "rfq-awaiting-accept",
                        "dashboard.rfq.queues.awaitingAccept.title",
                        "dashboard.rfq.queues.awaitingAccept.subtitle",
                        buildRfqManagementHref(startDate, endDate, selectedFilters, Map.of("status", RfqStatus.NEW.name(), "isAccept", "false")),
                        rfqs.stream()
                                .filter(rfq -> rfq.getStatus() == RfqStatus.NEW)
                                .toList(),
                        "รอรับงาน",
                        false,
                        PROCUREMENT_VISIBLE_TO
                ),
                queue(
                        "rfq-in-progress",
                        "dashboard.rfq.queues.inProgress.title",
                        "dashboard.rfq.queues.inProgress.subtitle",
                        buildPriceInquiryManagementHref(startDate, endDate, selectedFilters, Map.of("status", RfqStatus.IN_PROGRESS.name())),
                        rfqs.stream()
                                .filter(rfq -> rfq.getStatus() == RfqStatus.IN_PROGRESS)
                                .toList(),
                        null,
                        true,
                        PROCUREMENT_VISIBLE_TO
                ),
                queue(
                        "rfq-requested-info",
                        "dashboard.rfq.queues.requestedInfo.title",
                        "dashboard.rfq.queues.requestedInfo.subtitle",
                        buildPriceInquiryManagementHref(startDate, endDate, selectedFilters, Map.of("status", RfqStatus.REQUESTED_INFO.name())),
                        rfqs.stream()
                                .filter(rfq -> rfq.getStatus() == RfqStatus.REQUESTED_INFO)
                                .toList(),
                        "ขอข้อมูลเพิ่มเติม",
                        true,
                        SALES_VISIBLE_TO
                ),
                queue(
                        "rfq-supplier-quoted",
                        "dashboard.rfq.queues.supplierQuoted.title",
                        "dashboard.rfq.queues.supplierQuoted.subtitle",
                        buildPriceInquiryManagementHref(startDate, endDate, selectedFilters, Map.of("status", RfqStatus.SUPPLIER_QUOTED.name())),
                        rfqs.stream()
                                .filter(rfq -> rfq.getStatus() == RfqStatus.SUPPLIER_QUOTED)
                                .toList(),
                        "ซัพตอบแล้ว",
                        true,
                        PROCUREMENT_VISIBLE_TO
                ),
                queue(
                        "rfq-awaiting-quotation",
                        "dashboard.rfq.queues.awaitingQuotation.title",
                        "dashboard.rfq.queues.awaitingQuotation.subtitle",
                        buildRfqManagementHref(startDate, endDate, selectedFilters, Map.of("status", RfqStatus.QUOTED.name())),
                        rfqs.stream()
                                .filter(rfq -> rfq.getStatus() == RfqStatus.QUOTED)
                                .filter(rfq -> !rfqIdsWithQuotation.contains(rfq.getId()))
                                .toList(),
                        "รอออกใบเสนอราคา",
                        false,
                        SALES_VISIBLE_TO
                ),
                queue(
                        "rfq-special-price",
                        "dashboard.rfq.queues.specialPrice.title",
                        "dashboard.rfq.queues.specialPrice.subtitle",
                        buildPriceInquiryManagementHref(startDate, endDate, selectedFilters, Map.of("status", RfqStatus.SPECIAL_PRICE_REVIEW.name())),
                        rfqs.stream()
                                .filter(rfq -> rfq.getStatus() == RfqStatus.SPECIAL_PRICE_REVIEW)
                                .toList(),
                        "ทบทวนราคาพิเศษ",
                        true,
                        ALL_RFQ_VISIBLE_TO
                )
        );
    }

    private List<DashboardQuickLinkDto> buildQuickLinks(
            String roleCode,
            LocalDate startDate,
            LocalDate endDate,
            Map<String, String> selectedFilters
    ) {
        List<DashboardQuickLinkDto> links = new ArrayList<>();
        if (RFQ_CREATE_ROLES.contains(StringUtils.defaultString(roleCode))) {
            links.add(quickLink("create-rfq", "dashboard.rfq.quickLinks.create.title", "dashboard.rfq.quickLinks.create.subtitle", RFQ_CREATE_PATH, "rfq"));
        }
        links.add(quickLink(
                "rfq-list",
                "dashboard.rfq.quickLinks.rfqList.title",
                "dashboard.rfq.quickLinks.rfqList.subtitle",
                buildRfqManagementHref(startDate, endDate, selectedFilters, Map.of()),
                "rfq"
        ));
        links.add(quickLink(
                "price-inquiry-list",
                "dashboard.rfq.quickLinks.priceInquiry.title",
                "dashboard.rfq.quickLinks.priceInquiry.subtitle",
                buildPriceInquiryManagementHref(startDate, endDate, selectedFilters, Map.of()),
                "quotation"
        ));
        return links;
    }

    private DashboardMetricDto metric(String id, String title, long value, String subtitle, String trend, String tone, String href, List<String> visibleTo) {
        DashboardMetricDto metric = new DashboardMetricDto();
        metric.setId(id);
        metric.setTitle(title);
        metric.setValue(String.valueOf(value));
        metric.setSubtitle(subtitle);
        metric.setTrend(trend);
        metric.setTone(tone);
        metric.setHref(href);
        metric.setVisibleTo(visibleTo);
        return metric;
    }

    private DashboardSeriesDto series(
            String name,
            String color,
            List<LocalDate> dates,
            Map<LocalDate, List<RfqHeaderEntity>> rfqByDate,
            Function<List<RfqHeaderEntity>, Double> valueFunction
    ) {
        DashboardSeriesDto series = new DashboardSeriesDto();
        series.setName(name);
        series.setColor(color);
        series.setData(dates.stream()
                .map(date -> valueFunction.apply(rfqByDate.getOrDefault(date, List.of())))
                .toList());
        return series;
    }

    private int getAcceptWorkDurationBucketIndex(long minutes) {
        if (minutes < 60) {
            return 0;
        }
        if (minutes < 180) {
            return 1;
        }
        if (minutes < 360) {
            return 2;
        }
        if (minutes < 1_440) {
            return 3;
        }
        if (minutes < 2_880) {
            return 4;
        }
        if (minutes < 4_320) {
            return 5;
        }
        return 6;
    }

    private Long resolveAcceptWorkDurationMinutes(
            RfqHeaderEntity rfq,
            Map<String, ZonedDateTime> inProgressTimelineByRfqId
    ) {
        if (rfq.getAcceptWorkDurationMinutes() != null) {
            return rfq.getAcceptWorkDurationMinutes();
        }

        ZonedDateTime inProgressAt = inProgressTimelineByRfqId.get(rfq.getId());
        if (rfq.getRequestedDate() == null || inProgressAt == null) {
            return null;
        }

        AppProperties.RfqPendingAcceptance acceptanceConfig = appProperties.getRfqPendingAcceptance();
        return businessDurationService.calculateBusinessDurationMinutesWithCutoff(
                rfq.getRequestedDate(),
                inProgressAt,
                acceptanceConfig != null ? acceptanceConfig.getCutoffTime() : null,
                acceptanceConfig != null ? acceptanceConfig.getNextDayStartTime() : null
        );
    }

    private Long resolveSupplierQuoteDurationMinutes(
            RfqHeaderEntity rfq,
            Map<String, ZonedDateTime> inProgressTimelineByRfqId,
            Map<String, ZonedDateTime> supplierQuotedTimelineByRfqId
    ) {
        if (rfq == null || inProgressTimelineByRfqId == null || supplierQuotedTimelineByRfqId == null) {
            return null;
        }

        ZonedDateTime inProgressAt = inProgressTimelineByRfqId.get(rfq.getId());
        ZonedDateTime supplierQuotedAt = supplierQuotedTimelineByRfqId.get(rfq.getId());
        if (inProgressAt == null || supplierQuotedAt == null) {
            return null;
        }

        return businessDurationService.calculateBusinessDurationMinutes(inProgressAt, supplierQuotedAt);
    }

    private Map<String, ZonedDateTime> buildTimelineMap(Collection<String> rfqIds, RfqStatus status) {
        if (rfqIds == null || rfqIds.isEmpty() || status == null) {
            return Map.of();
        }

        return rfqStatusTimelineRepository.findAllByIdRfqIdInAndIdStatusOrderByStatusDatetimeAsc(rfqIds, status).stream()
                .collect(Collectors.toMap(
                        timeline -> timeline.getId().getRfqId(),
                        RfqStatusTimelineEntity::getStatusDatetime,
                        (left, right) -> left.isBefore(right) ? left : right,
                        LinkedHashMap::new
                ));
    }

    private DashboardDistributionChartDto distributionChart(
            String id,
            String title,
            String subtitle,
            List<DashboardDistributionItemDto> items,
            List<String> visibleTo
    ) {
        DashboardDistributionChartDto chart = new DashboardDistributionChartDto();
        chart.setId(id);
        chart.setTitle(title);
        chart.setSubtitle(subtitle);
        chart.setItems(items);
        chart.setVisibleTo(visibleTo);
        return chart;
    }

    private DashboardWorkQueueDto queue(
            String id,
            String title,
            String subtitle,
            String href,
            List<RfqHeaderEntity> rfqs,
            String defaultStatus,
            boolean usePriceInquiryDetail,
            List<String> visibleTo
    ) {
        DashboardWorkQueueDto queue = new DashboardWorkQueueDto();
        queue.setId(id);
        queue.setTitle(title);
        queue.setSubtitle(subtitle);
        queue.setHref(href);
        queue.setCount((long) rfqs.size());
        queue.setVisibleTo(visibleTo);
        queue.setItems(rfqs.stream()
                .sorted(Comparator.comparing(RfqHeaderEntity::getRequestedDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .map(rfq -> queueItem(rfq, defaultStatus, usePriceInquiryDetail))
                .toList());
        return queue;
    }

    private DashboardQueueItemDto queueItem(RfqHeaderEntity rfq, String defaultStatus, boolean usePriceInquiryDetail) {
        DashboardQueueItemDto item = new DashboardQueueItemDto();
        item.setId(rfq.getId());
        item.setTitle(rfq.getId());
        item.setMeta(buildQueueMeta(rfq));
        item.setStatus(StringUtils.defaultIfBlank(defaultStatus, displayRfqStatus(rfq.getStatus())));
        item.setHref((usePriceInquiryDetail ? "/price-inquiry/" : "/rfq/") + rfq.getId());
        return item;
    }

    private DashboardQuickLinkDto quickLink(String id, String title, String description, String href, String icon) {
        DashboardQuickLinkDto link = new DashboardQuickLinkDto();
        link.setId(id);
        link.setTitle(title);
        link.setDescription(description);
        link.setHref(href);
        link.setIcon(icon);
        return link;
    }

    private List<DashboardDistributionItemDto> buildBreakdown(
            List<RfqHeaderEntity> rfqs,
            Function<RfqHeaderEntity, String> classifier,
            int limit
    ) {
        return buildBreakdown(rfqs, classifier, null, limit);
    }

    private List<DashboardDistributionItemDto> buildBreakdown(
            List<RfqHeaderEntity> rfqs,
            Function<RfqHeaderEntity, String> classifier,
            Function<String, String> colorResolver,
            int limit
    ) {
        List<Map.Entry<String, Long>> entries = rfqs.stream()
                .map(classifier)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .toList();

        List<DashboardDistributionItemDto> items = new ArrayList<>();
        for (int index = 0; index < entries.size(); index++) {
            Map.Entry<String, Long> entry = entries.get(index);
            DashboardDistributionItemDto item = new DashboardDistributionItemDto();
            item.setLabel(entry.getKey());
            item.setValue(entry.getValue());
            item.setColor(colorResolver != null ? colorResolver.apply(entry.getKey()) : distributionPaletteColor(index));
            items.add(item);
        }
        return items;
    }

    private String distributionPaletteColor(int index) {
        return DISTRIBUTION_PALETTE.get(Math.floorMod(index, DISTRIBUTION_PALETTE.size()));
    }

    private long countByStatus(List<RfqHeaderEntity> rfqs, RfqStatus status) {
        return rfqs.stream().filter(rfq -> rfq.getStatus() == status).count();
    }

    private long countOverdueSla(List<RfqHeaderEntity> rfqs, LocalDate today) {
        return rfqs.stream()
                .filter(rfq -> rfq.getStatus() == RfqStatus.NEW || rfq.getStatus() == RfqStatus.IN_PROGRESS)
                .filter(rfq -> rfq.getSlaDate() != null)
                .filter(rfq -> rfq.getSlaDate().withZoneSameInstant(DateUtil.getTimeZone()).toLocalDate().isBefore(today))
                .count();
    }

    private String buildQueueMeta(RfqHeaderEntity rfq) {
        String customerName = rfq.getCustomer() != null
                ? StringUtils.defaultIfBlank(rfq.getCustomer().getCustomerName(), rfq.getCustomer().getCompanyName())
                : "-";
        String requestedDate = rfq.getRequestedDate() == null
                ? "-"
                : rfq.getRequestedDate().withZoneSameInstant(DateUtil.getTimeZone()).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        return customerName + " / " + requestedDate;
    }

    private String getProductFamilyLabel(RfqHeaderEntity rfq) {
        if (rfq.getProductFamilyEntity() != null) {
            return StringUtils.defaultIfBlank(rfq.getProductFamilyEntity().getNameTh(), rfq.getProductFamilyEntity().getNameEn());
        }
        return StringUtils.defaultIfBlank(rfq.getProductFamily(), "-");
    }

    private String getProductSubtype1Label(RfqHeaderEntity rfq) {
        if (rfq.getProductUsage() != null) {
            return StringUtils.defaultIfBlank(rfq.getProductUsage().getNameTh(), rfq.getProductUsage().getNameEn());
        }
        return "-";
    }

    private String getEmployeeLabel(EmployeeEntity employee) {
        if (employee == null) {
            return "-";
        }
        return StringUtils.defaultIfBlank(employee.getNickName(),
                StringUtils.trimToEmpty(employee.getFirstNameTh()) + " " + StringUtils.trimToEmpty(employee.getLastNameTh())).trim();
    }

    private DashboardDistributionChartDto buildCustomerTypeCountChart(List<RfqHeaderEntity> rfqs, String roleCode, List<LocalDate> months) {
        if (rfqs == null || rfqs.isEmpty()) {
            return null;
        }

        List<DashboardDistributionItemDto> items = buildBreakdown(
                rfqs,
                this::getCustomerTypeLabel,
                12
        );
        if (items.isEmpty()) {
            return null;
        }

        DashboardDistributionChartDto chart = new DashboardDistributionChartDto();
        chart.setId("rfq-by-customer-type-count");
        chart.setTitle("dashboard.rfq.charts.byCustomerTypeCount.title");
        chart.setSubtitle("dashboard.rfq.charts.byCustomerTypeCount.subtitle");
        chart.setItems(items);
        chart.setSeries(buildMonthlyBreakdown(rfqs, months, items, this::getCustomerTypeLabel));
        chart.setVisibleTo(ALL_RFQ_VISIBLE_TO);
        return chart;
    }

    private String getCustomerTypeLabel(RfqHeaderEntity rfq) {
        if (rfq == null || rfq.getCustomer() == null || StringUtils.isBlank(rfq.getCustomer().getId())) {
            return "ยังไม่เป็นลูกค้า";
        }

        CustomerEntity customer = rfq.getCustomer();
        if (customer.getCustomerTier() == null) {
            return "ไม่ระบุระดับลูกค้า";
        }

        return StringUtils.defaultIfBlank(customer.getCustomerTier().getNameEn(),
                customer.getCustomerTier().getId().getCode()
        );
    }

    private String displayRfqStatus(RfqStatus status) {
        if (status == null) {
            return "-";
        }
        return switch (status) {
            case NEW -> "ใหม่";
            case IN_PROGRESS -> "กำลังดำเนินการ";
            case SUPPLIER_QUOTED -> "ซัพตอบแล้ว";
            case REQUESTED_INFO -> "ขอข้อมูลเพิ่มเติม";
            case SPECIAL_PRICE_REVIEW -> "รอทบทวนราคาพิเศษ";
            case QUOTED -> "เสนอราคาแล้ว";
            case CLOSED -> "ปิดงาน";
            case COMPLETED -> "เสร็จสิ้น";
        };
    }

    private String statusColor(String status) {
        return switch (StringUtils.defaultString(status)) {
            case "ใหม่" -> "#2f80ed";
            case "กำลังดำเนินการ" -> "#56ccf2";
            case "ซัพตอบแล้ว" -> "#27ae60";
            case "ขอข้อมูลเพิ่มเติม" -> "#f2994a";
            case "รอทบทวนราคาพิเศษ" -> "#eb5757";
            case "เสนอราคาแล้ว" -> "#6fcf97";
            case "ยกเลิก", "ปฏิเสธ", "ปิดงาน" -> "#bdbdbd";
            case "เสร็จสิ้น" -> "#219653";
            default -> "#2d9cdb";
        };
    }

    private String buildRfqManagementHref(
            LocalDate startDate,
            LocalDate endDate,
            Map<String, String> selectedFilters,
            Map<String, String> filters
    ) {
        return buildHref(RFQ_MANAGEMENT_PATH, startDate, endDate, selectedFilters, filters);
    }

    private String buildPriceInquiryManagementHref(
            LocalDate startDate,
            LocalDate endDate,
            Map<String, String> selectedFilters,
            Map<String, String> filters
    ) {
        return buildHref(PRICE_INQUIRY_MANAGEMENT_PATH, startDate, endDate, selectedFilters, filters);
    }

    private String buildHref(
            String path,
            LocalDate startDate,
            LocalDate endDate,
            Map<String, String> selectedFilters,
            Map<String, String> filters
    ) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("requestedDateStart", startDate.toString());
        params.put("requestedDateEnd", endDate.toString());
        params.putAll(selectedFilters);
        params.putAll(filters);
        String query = params.entrySet().stream()
                .filter(entry -> StringUtils.isNotBlank(entry.getValue()))
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .collect(Collectors.joining("&"));
        return StringUtils.isBlank(query) ? path : path + "?" + query;
    }

    private String encode(String value) {
        return URLEncoder.encode(StringUtils.defaultString(value), StandardCharsets.UTF_8);
    }

    private List<LocalDate> enumerateMonths(LocalDate startDate, LocalDate endDate) {
        List<LocalDate> months = new ArrayList<>();
        LocalDate cursor = startDate.withDayOfMonth(1);
        LocalDate lastMonth = endDate.withDayOfMonth(1);
        while (!cursor.isAfter(lastMonth)) {
            months.add(cursor);
            cursor = cursor.plusMonths(1);
        }
        return months;
    }

    private LocalDate rfqMonth(RfqHeaderEntity rfq) {
        return rfq.getRequestedDate().withZoneSameInstant(DateUtil.getTimeZone())
                .toLocalDate().withDayOfMonth(1);
    }

    private DashboardSeriesDto monthSeries(LocalDate month, List<Double> values) {
        DashboardSeriesDto series = new DashboardSeriesDto();
        series.setName(month.toString().substring(0, 7));
        series.setColor(MONTH_PALETTE.get(month.getMonthValue() - 1));
        series.setData(values);
        return series;
    }

    private List<DashboardSeriesDto> buildMonthlyBreakdown(
            List<RfqHeaderEntity> rfqs,
            List<LocalDate> months,
            List<DashboardDistributionItemDto> items,
            Function<RfqHeaderEntity, String> classifier
    ) {
        Map<LocalDate, Map<String, Long>> counts = rfqs.stream()
                .filter(rfq -> rfq.getRequestedDate() != null && StringUtils.isNotBlank(classifier.apply(rfq)))
                .collect(Collectors.groupingBy(this::rfqMonth,
                        Collectors.groupingBy(classifier, Collectors.counting())));
        return months.stream().map(month -> monthSeries(month, items.stream()
                .map(item -> counts.getOrDefault(month, Map.of()).getOrDefault(item.getLabel(), 0L).doubleValue())
                .toList())).toList();
    }

    private LocalDate parseLocalDateOrDefault(String value, LocalDate fallback) {
        if (StringUtils.isBlank(value)) {
            return fallback;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception exception) {
            log.warn("Cannot parse dashboard date '{}', fallback to {}", value, fallback);
            return fallback;
        }
    }
}
