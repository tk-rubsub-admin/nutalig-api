package com.nutalig.utils;

import com.nutalig.entity.RfqDetailEntity;
import com.nutalig.entity.RfqHeaderEntity;
import com.nutalig.entity.RfqTierEntity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import static com.nutalig.utils.ObjectUtil.safeValue;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RfqUtil {

    private static DecimalFormat df = new DecimalFormat("#,##0");
    private static DecimalFormat df2 = new DecimalFormat("#,##0.00");
    public static String displayProductFamily(RfqHeaderEntity rfq) {
        if (rfq.getProductFamilyEntity() != null) {
            return safeValue(rfq.getProductFamilyEntity().getNameTh() != null ? rfq.getProductFamilyEntity().getNameTh() : rfq.getProductFamilyEntity().getNameTh())
                    + " (" + safeValue(rfq.getProductFamilyEntity().getCode()) + ")";
        }
        return safeValue(rfq.getProductFamily());
    }

    public static String displayProductSubtype1(RfqHeaderEntity rfq) {
        if (rfq.getProductUsage() != null) {
            return safeValue(rfq.getProductUsage().getNameTh() != null ? rfq.getProductUsage().getNameTh() : rfq.getProductUsage().getNameTh())
                    + " (" + safeValue(rfq.getProductUsage().getCode()) + ")";
        }
        return "-";
    }

    public static String displayProductSubtype2(RfqHeaderEntity rfq) {
        if (rfq.getSystemMechanic() != null) {
            return safeValue(rfq.getSystemMechanic().getNameTh() != null ? rfq.getSystemMechanic().getNameTh() : rfq.getSystemMechanic().getNameTh())
                    + " (" + safeValue(rfq.getSystemMechanic().getCode()) + ")";
        }
        return "-";
    }

    public static String displayProductMaterial(RfqHeaderEntity rfq) {
        if (rfq.getMaterial() != null) {
            return safeValue(rfq.getMaterial().getNameTh() != null ? rfq.getMaterial().getNameTh() : rfq.getMaterial().getNameTh())
                    + " (" + safeValue(rfq.getMaterial().getCode()) + ")";
        }
        return safeValue(rfq.getMaterialCode());
    }

    public static String buildCustomerDetailSection(RfqHeaderEntity rfq) {
        if (rfq.getDetails() == null || rfq.getDetails().isEmpty()) {
            return "-";
        }
        List<String> lines = new ArrayList<>();
        for (RfqDetailEntity detail : rfq.getDetails()) {
            lines.add(detail.getOptionName());
            lines.add("สเปกสินค้า : " + detail.getSpec());
            lines.add("——————————————");
            lines.add("ราคาเสนอ");
            lines.add(buildCustomerQuotedTiers(detail));
            lines.add("===========");
            lines.add("");
        }
        return String.join("\n", lines);
    }

    public static String buildCustomerQuotedTiers(RfqDetailEntity rfqDetail) {
        if (rfqDetail == null || rfqDetail.getTiers().isEmpty()) {
            return "-";
        }

        List<String> lines = new ArrayList<>();
        for (RfqTierEntity tier : rfqDetail.getTiers()) {
            StringBuilder tierLine = new StringBuilder("\n");
            tierLine.append("จำนวน ");
            tierLine.append(df.format(tier.getQuantity()));
            tierLine.append("\n");

            if (tier.getSeaFreightCost() != null || BigDecimal.ZERO.compareTo(tier.getSeaFreightCost()) < 1) {
                tierLine.append(tier.getIsFcl() ? "ส่งทางเรือแบบปิดตู้: " : "ส่งทางเรือ: ");
                tierLine.append(df2.format(tier.getSeaTotalPrice()));
                tierLine.append("\n");
            }

            if (tier.getLandFreightCost() != null || BigDecimal.ZERO.compareTo(tier.getLandFreightCost()) < 1) {
                tierLine.append("ส่งทางรถ: ");
                tierLine.append(df2.format(tier.getLandTotalPrice()));
            }
            lines.add(tierLine.toString());
        }

        return  String.join("\n", lines);
    }

    public static String buildCustomerQuotedRequestedDate(RfqHeaderEntity rfq) {
        if (rfq.getQuotedDate() == null) {
            return "";
        }
        return DateUtil.DD_MM_YY.format(rfq.getQuotedDate());
    }

}
