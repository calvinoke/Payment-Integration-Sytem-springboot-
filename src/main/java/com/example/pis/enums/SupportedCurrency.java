package com.example.pis.enums;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum SupportedCurrency {
    AED, AFN, ALL, AMD, ANG, AOA, ARS, AUD, AWG, AZN,
    BAM, BBD, BDT, BGN, BIF, BMD, BND, BOB, BRL, BSD,
    BWP, BYN, BZD, CAD, CDF, CHF, CLP, CNY, COP, CRC,
    CVE, CZK, DJF, DKK, DOP, DZD, EGP, ETB, EUR, FJD,
    FKP, GBP, GEL, GIP, GMD, GNF, GTQ, GYD, HKD, HNL,
    HRK, HTG, HUF, IDR, ILS, INR, ISK, JMD, JPY, KES,
    KGS, KHR, KMF, KRW, KYD, KZT, LAK, LBP, LKR, LRD,
    LSL, MAD, MDL, MGA, MKD, MMK, MNT, MOP, MRU, MUR,
    MVR, MWK, MXN, MYR, MZN, NAD, NGN, NIO, NOK, NPR,
    NZD, PAB, PEN, PGK, PHP, PKR, PLN, PYG, QAR, RON,
    RSD, RUB, RWF, SAR, SBD, SCR, SEK, SGD, SHP, SLL,
    SOS, SRD, STN, SZL, THB, TJS, TOP, TRY, TTD, TWD,
    TZS, UAH, UGX, USD, UYU, UZS, VND, VUV, WST, XAF,
    XCD, XOF, XPF, YER, ZAR, ZMW;

    public static final Set<String> CODES =
        Arrays.stream(values())
              .map(Enum::name)
              .collect(Collectors.toUnmodifiableSet());

    public static boolean isSupported(String code) {
        return code != null && CODES.contains(code.toUpperCase());
    }
}
