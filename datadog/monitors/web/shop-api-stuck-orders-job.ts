import { Construct } from "constructs";
import { Environment } from "../common/environments";
import { getErrorRecipients } from "../recipients";
import { Monitor } from "../common/monitor";
import { Comparator, metric_sum_query, trace_analytics_count_query } from "../common/queries";

export class ShopApiStuckOrdersJobMonitors extends Construct {
    constructor(scope: Construct, environment: Environment) {
        const serviceName = 'web-shop-api-stuck-orders-job';
        const stuckOrdersUsecase = "StuckOrdersUsecase";

        super(scope, `${serviceName}_${environment}`);
        const trace_alert_config = {
            recipients: getErrorRecipients(environment),
            type: "trace-analytics alert",
            monitorThresholds: {
                critical: "3",
                warning: "1",
            },
        }

        const executionRateConfig = {
            recipients: getErrorRecipients(environment),
            type: "metric alert",
            monitorThresholds: {
                critical: "1",
                warning: "2",
            },
        }
        const executionRateWindow = "15m";

        const statuses = [
            'awaiting_fulfillment',
            'awaiting_payment',
            'awaiting_shipment',
            'awaiting_pickup',
            'declined',
            'disputed',
            'manual-verification_required',
            'partially_shipped',
            'shipped'
        ]

        const window = "5m";
        const common_query = `service:${serviceName} env:${environment}`;

        const error_query = `${common_query} status:error`;
        const warn_query = `$${common_query} status:warn`;
        const tags = [serviceName, `env:${environment}`];

        new Monitor(this, "service_error_rate_high", {
            query: trace_analytics_count_query(
                `${error_query}`,
                window,
                trace_alert_config.monitorThresholds.critical,
            ),
            name: `[${serviceName}] Error rate too high`,
            message:
                `[${serviceName}]: throughput deviated too much from its usual value.`,
            tags: tags,
            ...trace_alert_config,
        });

        new Monitor(this, "service_warning_rate_high", {
            query: trace_analytics_count_query(
                `${warn_query}`,
                window,
                trace_alert_config.monitorThresholds.critical,
            ),
            name: `[${serviceName}] Warning rate too high`,
            message:
                `[${serviceName}]: throughput deviated too much from its usual value.`,
            tags: tags,
            ...trace_alert_config,
        });

        for (const status in statuses) {
            const resourceName = `${stuckOrdersUsecase}_${status}#run`

            new Monitor(this, `service_error_rate_high_${status}`, {
                query: trace_analytics_count_query(
                    `${error_query} resource_name:${resourceName}`,
                    window,
                    trace_alert_config.monitorThresholds.critical,
                ),
                name: `[${serviceName}: ${status}] Error rate too high`,
                message:
                    `[${serviceName}: ${status}]: throughput deviated too much from its usual value.`,
                tags: tags,
                ...trace_alert_config,
            });

            new Monitor(this, `service_warning_rate_high_${status}`, {
                query: trace_analytics_count_query(
                    `${warn_query} resource_name:${resourceName}`,
                    window,
                    trace_alert_config.monitorThresholds.critical,
                ),
                name: `[${serviceName}: ${status}] Warning rate too high`,
                message:
                    `[${serviceName}: ${status}]: throughput deviated too much from its usual value.`,
                tags: tags,
                ...trace_alert_config,
            });

            new Monitor(this, `stuck_orders_execution_rate_too_low_${status}`, {
                query: metric_sum_query(
                    `sum:trace.${stuckOrdersUsecase}_${status}_run.hits{service:${serviceName},env:${environment}}.as_count()`,
                    executionRateWindow,
                    executionRateConfig.monitorThresholds.critical,
                    Comparator.Below
                ),
                name: `[${serviceName}] Execution rate too low for ${stuckOrdersUsecase}`,
                message:
                    `[${serviceName}]: Periodic job's execution rate is too low for ${stuckOrdersUsecase}.`,
                tags: tags,
                ...executionRateConfig,
            });
        }
    }
}