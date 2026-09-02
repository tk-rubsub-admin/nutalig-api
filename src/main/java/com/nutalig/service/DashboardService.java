package com.nutalig.service;

import com.nutalig.constant.RfqStatus;
import com.nutalig.config.AppProperties;
import com.nutalig.dto.*;
import com.nutalig.entity.*;
import com.nutalig.repository.QuotationRepository;
import com.nutalig.repository.RequestPriceHeaderRepository;
import com.nutalig.repository.RfqStatusTimelineRepository;
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

    private static final DateTimeFormatter REQUEST_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM");
    private static final List<String> DISTRIBUTION_PALETTE = List.of(
            "#2f80ed",
            "#27ae60",
            "#f2994a",
            "#9b51e0",
            "#eb5757",
            "#56ccf2",
            "#f2c94c",
            "#6fcf97",
            "#bb6bd9",
            "#f299c1"
    );
    private static final List<RfqStatus> IN_PROGRESS_QUEUE_STATUSES = List.of(RfqStatus.IN_PROGRESS, RfqStatus.REQUESTED_INFO);
    private static final Set<String> RFQ_CREATE_ROLES = Set.of("SUPER_ADMIN", "ADMIN", "SALES", "SALES_ADMIN");
    private static final List<String> SALES_VISIBLE_TO = List.of("SUPER_ADMIN", "ADMIN", "SALES", "SALES_ADMIN");
    private static final List<String> PROCUREMENT_VISIBLE_TO = List.of("SUPER_ADMIN", "ADMIN", "PROCUREMENT", "PROCUREMENT_ADMIN");
    private static final List<String> ALL_RFQ_VISIBLE_TO = List.of("SUPER_ADMIN", "ADMIN", "SALES", "SALES_ADMIN", "PROCUREMENT", "PROCUREMENT_ADMIN");
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
        if (StringUtils.equals(roleCode, "SALES") && employeeId != null) {
            specification = specification.and(salesIdEqual(employeeId));
        } else if (StringUtils.equals(roleCode, "PROCUREMENT") && employeeId != null) {
            specification = specification.and(procurementIdEqual(employeeId));
        }
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

        DashboardDataDto dashboard = new DashboardDataDto();
        dashboard.setRange("CUSTOM");
        dashboard.setDateFrom(startDate.toString());
        dashboard.setDateTo(endDate.toString());
        dashboard.setGeneratedAt(ZonedDateTime.now(DateUtil.getTimeZone()));
        dashboard.setSource("api");
        dashboard.setMetrics(buildMetrics(rfqs, startDate, endDate, selectedFilters));
        dashboard.setTrendCharts(buildTrendCharts(rfqs, startDate, endDate, roleCode));
        dashboard.setAcceptWorkDurationChart(buildAcceptWorkDurationChart(rfqs, roleCode));
        dashboard.setSupplierQuoteDurationChart(buildSupplierQuoteDurationChart(rfqs, roleCode));
        dashboard.setSalesCountChart(buildSalesCountChart(rfqs, roleCode));
        dashboard.setCustomerTypeCountChart(buildCustomerTypeCountChart(rfqs, roleCode));
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
                        PROCUREMENT_VISIBLE_TO
                ),
                metric(
                        "rfq-in-progress",
                        "dashboard.rfq.metrics.inProgress.title",
                        countByStatus(rfqs, RfqStatus.IN_PROGRESS),
                        "dashboard.rfq.metrics.inProgress.subtitle",
                        null,
                        "info",
                        buildPriceInquiryManagementHref(startDate, endDate, selectedFilters, Map.of("statuses", RfqStatus.IN_PROGRESS.name())),
                        PROCUREMENT_VISIBLE_TO
                ),
                metric(
                        "rfq-supplier-quoted",
                        "dashboard.rfq.metrics.supplierQuoted.title",
                        countByStatus(rfqs, RfqStatus.SUPPLIER_QUOTED),
                        "dashboard.rfq.metrics.supplierQuoted.subtitle",
                        null,
                        "neutral",
                        buildRfqManagementHref(startDate, endDate, selectedFilters, Map.of("statuses", RfqStatus.SUPPLIER_QUOTED.name())),
                        SALES_VISIBLE_TO
                ),
                metric(
                        "rfq-quoted",
                        "dashboard.rfq.metrics.quoted.title",
                        countByStatus(rfqs, RfqStatus.QUOTED),
                        "dashboard.rfq.metrics.quoted.subtitle",
                        null,
                        "neutral",
                        buildRfqManagementHref(startDate, endDate, selectedFilters, Map.of("statuses", RfqStatus.QUOTED.name())),
                        SALES_VISIBLE_TO
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
                        PROCUREMENT_VISIBLE_TO
                ),
                metric(
                        "rfq-rejected",
                        "dashboard.rfq.metrics.rejected.title",
                        countByStatus(rfqs, RfqStatus.REJECTED),
                        "dashboard.rfq.metrics.rejected.subtitle",
                        null,
                        "error",
                        buildPriceInquiryManagementHref(startDate, endDate, selectedFilters, Map.of("statuses", RfqStatus.REJECTED.name())),
                        PROCUREMENT_VISIBLE_TO
                ),
                metric(
                        "rfq-other",
                        "dashboard.rfq.metrics.other.title",
                        countByStatus(rfqs, RfqStatus.CANCELED) + countByStatus(rfqs, RfqStatus.REQUESTED_INFO) + countByStatus(rfqs, RfqStatus.CLOSED),
                        "dashboard.rfq.metrics.other.subtitle",
                        null,
                        "warning",
                        buildPriceInquiryManagementHref(
                                startDate,
                                endDate,
                                selectedFilters,
                                Map.of("statuses", String.join(",",
                                        RfqStatus.CANCELED.name(),
                                        RfqStatus.REQUESTED_INFO.name(),
                                        RfqStatus.CLOSED.name()))
                        ),
                        PROCUREMENT_VISIBLE_TO
                )
        );
    }

    private List<DashboardTrendChartDto> buildTrendCharts(
            List<RfqHeaderEntity> rfqs,
            LocalDate startDate,
            LocalDate endDate,
            String roleCode
    ) {
        List<LocalDate> dates = enumerateDates(startDate, endDate);
        Map<LocalDate, List<RfqHeaderEntity>> rfqByDate = rfqs.stream()
                .filter(rfq -> rfq.getRequestedDate() != null)
                .collect(Collectors.groupingBy(
                        rfq -> rfq.getRequestedDate().withZoneSameInstant(DateUtil.getTimeZone()).toLocalDate(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        DashboardTrendChartDto volumeChart = new DashboardTrendChartDto();
        volumeChart.setId("rfq-volume");
        volumeChart.setTitle("dashboard.rfq.charts.volume.title");
        volumeChart.setSubtitle("dashboard.rfq.charts.volume.subtitle");
        volumeChart.setUnit("COUNT");
        volumeChart.setLabels(dates.stream().map(date -> date.format(REQUEST_DATE_FORMAT)).toList());
        volumeChart.setSeries(List.of(
                series("งานปกติ", "#2f80ed", dates, rfqByDate, items -> (double) items.stream().filter(item -> Boolean.FALSE.equals(item.getUrgentRequest()) || item.getUrgentRequest() == null).count()),
                series("งานเร่งด่วน", "#f2994a", dates, rfqByDate, items -> (double) items.stream().filter(item -> Boolean.TRUE.equals(item.getUrgentRequest())).count())
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
                series("ใหม่", "#56ccf2", dates, rfqByDate, items -> (double) items.stream().filter(item -> item.getStatus() == RfqStatus.NEW).count()),
                series("กำลังดำเนินการ", "#2f80ed", dates, rfqByDate, items -> (double) items.stream().filter(item -> item.getStatus() == RfqStatus.IN_PROGRESS).count()),
                series("ซัพตอบแล้ว", "#27ae60", dates, rfqByDate, items -> (double) items.stream().filter(item -> item.getStatus() == RfqStatus.SUPPLIER_QUOTED).count()),
                series("เสนอราคาแล้ว", "#6fcf97", dates, rfqByDate, items -> (double) items.stream().filter(item -> item.getStatus() == RfqStatus.QUOTED).count())
        ));
        stageChart.setVisibleTo(StringUtils.equals(roleCode, "PROCUREMENT") ? PROCUREMENT_VISIBLE_TO : SALES_VISIBLE_TO);

        return List.of(volumeChart, stageChart);
    }

    private DashboardTrendChartDto buildAcceptWorkDurationChart(List<RfqHeaderEntity> rfqs, String roleCode) {
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

        long[] bucketCounts = new long[8];
        for (RfqHeaderEntity rfq : eligibleRfqs) {
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
        DashboardSeriesDto series = new DashboardSeriesDto();
        series.setName("จำนวน RFQ");
        series.setColor("#f2994a");
        series.setData(Arrays.stream(bucketCounts)
                .boxed()
                .map(Long::doubleValue)
                .toList());
        chart.setSeries(List.of(series));
        chart.setVisibleTo(ALL_RFQ_VISIBLE_TO);
        return chart;
    }

    private DashboardTrendChartDto buildSupplierQuoteDurationChart(List<RfqHeaderEntity> rfqs, String roleCode) {
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

        long[] bucketCounts = new long[8];
        long counted = 0;
        for (RfqHeaderEntity rfq : eligibleRfqs) {
            Long durationMinutes = resolveSupplierQuoteDurationMinutes(rfq, inProgressTimelineByRfqId, supplierQuotedTimelineByRfqId);
            if (durationMinutes == null) {
                continue;
            }

            bucketCounts[getAcceptWorkDurationBucketIndex(durationMinutes)]++;
            counted++;
        }
        bucketCounts[7] = Math.max(eligibleRfqs.size() - counted, 0);

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
        DashboardSeriesDto series = new DashboardSeriesDto();
        series.setName("จำนวน RFQ");
        series.setColor("#f2994a");
        series.setData(Arrays.stream(bucketCounts)
                .boxed()
                .map(Long::doubleValue)
                .toList());
        chart.setSeries(List.of(series));
        chart.setVisibleTo(ALL_RFQ_VISIBLE_TO);
        return chart;
    }

    private List<DashboardDistributionChartDto> buildDistributionCharts(List<RfqHeaderEntity> rfqs, String roleCode) {
        List<DashboardDistributionChartDto> charts = new ArrayList<>();

        charts.add(distributionChart(
                "rfq-by-family",
                "dashboard.rfq.charts.family.title",
                "dashboard.rfq.charts.family.subtitle",
                buildBreakdown(rfqs, this::getProductFamilyLabel, 6),
                ALL_RFQ_VISIBLE_TO
        ));

        charts.add(distributionChart(
                "rfq-by-channel",
                "dashboard.rfq.charts.channel.title",
                "dashboard.rfq.charts.channel.subtitle",
                buildBreakdown(
                        rfqs,
                        rfq -> StringUtils.defaultIfBlank(StringUtils.trimToNull(rfq.getContactChannel()), "-"),
                        8
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

        charts.add(distributionChart(
                "rfq-sla-status",
                "dashboard.rfq.charts.sla.title",
                "dashboard.rfq.charts.sla.subtitle",
                buildSlaBreakdown(rfqs),
                PROCUREMENT_VISIBLE_TO
        ));

        return charts;
    }

    private DashboardDistributionChartDto buildSalesCountChart(List<RfqHeaderEntity> rfqs, String roleCode) {
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

        return businessDurationService.calculateBusinessDurationMinutesWithCutoff(
                rfq.getRequestedDate(),
                inProgressAt,
                appProperties.getRfqPendingAcceptance() != null
                        ? appProperties.getRfqPendingAcceptance().getCutoffTime()
                        : null
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

    private List<DashboardDistributionItemDto> buildSlaBreakdown(List<RfqHeaderEntity> rfqs) {
        LocalDate today = LocalDate.now(DateUtil.getTimeZone());
        long overdue = 0;
        long dueToday = 0;
        long onTrack = 0;

        for (RfqHeaderEntity rfq : rfqs) {
            if (!(rfq.getStatus() == RfqStatus.NEW || rfq.getStatus() == RfqStatus.IN_PROGRESS) || rfq.getSlaDate() == null) {
                continue;
            }
            LocalDate slaDate = rfq.getSlaDate().withZoneSameInstant(DateUtil.getTimeZone()).toLocalDate();
            if (slaDate.isBefore(today)) {
                overdue++;
            } else if (slaDate.isEqual(today)) {
                dueToday++;
            } else {
                onTrack++;
            }
        }

        List<DashboardDistributionItemDto> items = new ArrayList<>();
        items.add(distributionItem("Overdue", overdue, "#eb5757"));
        items.add(distributionItem("Due Today", dueToday, "#f2994a"));
        items.add(distributionItem("On Track", onTrack, "#27ae60"));
        return items;
    }

    private DashboardDistributionItemDto distributionItem(String label, long value, String color) {
        DashboardDistributionItemDto item = new DashboardDistributionItemDto();
        item.setLabel(label);
        item.setValue(value);
        item.setColor(color);
        return item;
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

    private String getEmployeeLabel(EmployeeEntity employee) {
        if (employee == null) {
            return "-";
        }
        return StringUtils.defaultIfBlank(employee.getNickName(),
                StringUtils.trimToEmpty(employee.getFirstNameTh()) + " " + StringUtils.trimToEmpty(employee.getLastNameTh())).trim();
    }

    private DashboardDistributionChartDto buildCustomerTypeCountChart(List<RfqHeaderEntity> rfqs, String roleCode) {
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
            case CANCELED -> "ยกเลิก";
            case REJECTED -> "ปฏิเสธ";
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

    private List<LocalDate> enumerateDates(LocalDate startDate, LocalDate endDate) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            dates.add(cursor);
            cursor = cursor.plusDays(1);
        }
        return dates;
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
