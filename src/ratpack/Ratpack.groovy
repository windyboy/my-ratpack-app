import me.windy.ratpack.book.*
import ratpack.groovy.template.MarkupTemplateModule
import com.zaxxer.hikari.HikariConfig
import ratpack.hikari.HikariModule
import ratpack.hystrix.HystrixModule
import ratpack.groovy.sql.SqlModule
import ratpack.config.ConfigData
import static ratpack.groovy.Groovy.groovyMarkupTemplate
import static ratpack.groovy.Groovy.ratpack
import ratpack.rx.RxRatpack
import ratpack.dropwizard.metrics.DropwizardMetricsConfig
import ratpack.dropwizard.metrics.DropwizardMetricsModule
import ratpack.dropwizard.metrics.MetricsWebsocketBroadcastHandler
import ratpack.hystrix.HystrixMetricsEventStreamHandler
import ratpack.health.HealthCheckHandler
import ratpack.server.Service
import ratpack.server.StartEvent
import ratpack.error.ServerErrorHandler
import ratpack.handling.RequestLogger
import org.slf4j.Logger
import org.slf4j.LoggerFactory
final Logger logger = LoggerFactory.getLogger(Ratpack.class)

ratpack {
  bindings {
    ConfigData configData = ConfigData.of { c ->
            c.props("$serverConfig.baseDir.file/application.properties")
            c.env()
            c.sysProps()
    }
    module MarkupTemplateModule
    module HikariModule, { HikariConfig c ->
      c.addDataSourceProperty("URL", "jdbc:mariadb://localhost/ratpack")
      c.setDataSourceClassName("org.mariadb.jdbc.MariaDbDataSource")
      // c.driverClassName = 'org.mariadb.jdbc.Driver'
      // c.jdbcUrl = 'jdbc:mariadb://localhost/ratpack'
      c.username = 'root'
      c.password = 'windyboy'
    }
    // module HikariModule
    module SqlModule
    module BookModule
    module new HystrixModule().sse()
    moduleConfig(DropwizardMetricsModule, configData.get("/metrics", DropwizardMetricsConfig))
    bind DatabaseHealthCheck

    bindInstance Service, new Service() {
            @Override
            void onStart(StartEvent event) throws Exception {
                logger.info "Initializing RX"
                RxRatpack.initialize()
                // event.registry.get(BookService).createTable()
            }
        }
    bind ServerErrorHandler, ErrorHandler
  }

  handlers { BookService bookService ->
    all RequestLogger.ncsa(logger) // log all requests
    get {
      render groovyMarkupTemplate("index.gtpl", title: "My Ratpack App")
    }

    prefix("admin") {

            get("health-check/:name?", new HealthCheckHandler())
            get("metrics-report", new MetricsWebsocketBroadcastHandler())

            get("metrics") {
                render groovyMarkupTemplate("metrics.gtpl", title: "Metrics")
            }
        }

        get("hystrix.stream", new HystrixMetricsEventStreamHandler())
    prefix("api/book") {
            all chain(registry.get(BookRestEndpoint))
    }

    files { dir "public" }
  }
}
