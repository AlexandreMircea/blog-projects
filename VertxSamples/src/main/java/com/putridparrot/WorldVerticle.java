package com.putridparrot;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.ServiceDiscoveryOptions;
import io.vertx.servicediscovery.impl.DiscoveryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorldVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorldVerticle.class);
    private static final String ROOT = "/world";

    private final Router router;
    private ServiceDiscovery discovery;
    private Record publishedRecord;

    public WorldVerticle(Router router) {
        this.router = router;
    }

    @Override
    public void start(Future<Void> future) {

        SharedVerticle
                .configuration(vertx)
                .setHandler(ar ->
        {
            if(ar.succeeded()) {
                JsonObject o  = ar.result();
                int port = o.getInteger("world.port", 8080);

                router.route(ROOT).handler(ctx -> {

                    LOGGER.debug("World called");

                    vertx.eventBus()
                            .publish(SharedVerticle.SVC_BUS, "World Service");

                    ctx.response()
                            .putHeader("content-type", "text/plain")
                            .end("World " + ctx.queryParam("name"));
                });


                vertx.createHttpServer()
                        .requestHandler(router::accept)
                        .listen(port, l ->
                        {
                            if(l.succeeded()) {
                                LOGGER.info("HTTP \"World\" server started on port " + port);

                                discovery = SharedVerticle.createServiceDiscovery(vertx);

                                SharedVerticle.publish(discovery,
                                        "hello-service",
                                        "localhost",
                                        port,
                                        ROOT)
                                        .setHandler(r ->
                                        {
                                            if(r.succeeded()) {
                                                publishedRecord = r.result();
                                            }
                                        });

                                future.succeeded();
                            }
                            else {
                                future.fail(l.cause());
                            }
                        });
             }
        });
    }

    @Override
    public void stop() {
        SharedVerticle.unpublish(discovery, publishedRecord);
    }}
