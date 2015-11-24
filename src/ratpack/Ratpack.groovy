import me.windy.ratpack.book.*
import ratpack.groovy.template.MarkupTemplateModule
import org.pac4j.http.client.FormClient
import org.pac4j.http.credentials.SimpleTestUsernamePasswordAuthenticator
import org.pac4j.http.profile.UsernameProfileCreator
import ratpack.pac4j.RatpackPac4j
import ratpack.session.SessionModule
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
    module SessionModule
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




    get("hystrix.stream", new HystrixMetricsEventStreamHandler())
    prefix("api/book") {
      all chain(registry.get(BookRestEndpoint))
    }

    def formClient = new FormClient()
    formClient.setLoginUrl("/login")
    formClient.setProfileCreator(new UsernameProfileCreator())
    formClient.setAuthenticator(new SimpleTestUsernamePasswordAuthenticator())

    def pac4jCallbackPath = "pac4j-callback"
    all (RatpackPac4j.authenticator(
        pac4jCallbackPath,
        formClient))

    get("login") { ctx ->
      render groovyMarkupTemplate("login.gtpl",
        title: "Login",
        action: "/$pac4jCallbackPath",
        method: 'get',
        buttonText: 'Login',
        error: request.queryParams.error ?: "")
    }

    get("logout") { ctx ->
        RatpackPac4j.logout(ctx).then {
            redirect("/")
        }
    }

    prefix("admin") {
        all(RatpackPac4j.requireAuth(FormClient.class))
        get("health-check/:name?", new HealthCheckHandler())
        get("metrics-report", new MetricsWebsocketBroadcastHandler())

        get("metrics") {
            render groovyMarkupTemplate("metrics.gtpl", title: "Metrics")
        }
    }

    files { dir "public" }
  }
}
