export interface TriggerRuleSummary {
    id: number;
    ruleType: string;
    thresholdValue: number | null;
    cooldownHours: number;
    active: boolean;
}

export interface VariantStatus {
    listingId: number;
    listingUrl: string;
    variantLabel: string | null;
    title: string | null;
    currency: string;
    observedAt: string;
    price: number | null;
    salePrice: number | null;
    effectivePrice: number | null;
    availability: string | null;
    underThreshold: boolean;
}

export interface WatchTargetOverview {
    id: number;
    name: string;
    category: string | null;
    brand: string | null;
    active: boolean;
    rules: TriggerRuleSummary[];
    variants: VariantStatus[];
}

export interface HistoryPoint {
    listingId: number;
    title: string | null;
    observedAt: string;
    price: number | null;
    salePrice: number | null;
    effectivePrice: number | null;
    availability: string | null;
}

export interface NotificationEventRow {
    id: number;
    watchTargetId: number;
    watchTargetName: string;
    listingId: number | null;
    ruleType: string;
    thresholdValue: number | null;
    observationEffectivePrice: number | null;
    sentAt: string;
}

export interface ObservationRow {
    id: number;
    listingId: number;
    observedAt: string;
    title: string | null;
    availability: string | null;
    price: number | null;
    salePrice: number | null;
    memberPrice: number | null;
    subscribePrice: number | null;
    quantityValue: number | null;
    quantityUnit: string | null;
    promoText: string | null;
}
