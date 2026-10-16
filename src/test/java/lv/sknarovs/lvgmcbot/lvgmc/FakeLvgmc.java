package lv.sknarovs.lvgmcbot.lvgmc;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.nio.charset.StandardCharsets.UTF_8;

/** Local stand-in for the LVĢMC API; unknown paths get 404. */
public final class FakeLvgmc implements AutoCloseable {

    private final HttpServer server;
    private final Map<String, String> responses = new ConcurrentHashMap<>();
    private final List<String> requests = new CopyOnWriteArrayList<>();

    FakeLvgmc() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/data/", this::handle);
        server.start();
    }

    /** Serves real responses recorded on 16 October 2026 (src/test/resources/lvgmc), forecasts for Rīga only. */
    public static FakeLvgmc withRecordedData() throws IOException {
        FakeLvgmc lvgmc = new FakeLvgmc();
        lvgmc.respond("weather_monitoring_points", Fixtures.read("weather_monitoring_points.json"));
        lvgmc.respond("weather_monitoring_data", Fixtures.read("weather_monitoring_data.json"));
        lvgmc.respond("weather_forecast_for_location_daily?nosaukums=Rīga", Fixtures.read("weather_forecast_for_location_daily_riga.json"));
        lvgmc.respond("weather_forecast_for_location_hourly?nosaukums=Rīga", Fixtures.read("weather_forecast_for_location_hourly_riga.json"));
        lvgmc.respond("sinopt_prognozes", Fixtures.read("sinopt_prognozes.json"));
        return lvgmc;
    }

    /** @param pathAndQuery after {@code /data/}, query decoded, e.g. {@code weather_forecast_for_location_daily?nosaukums=Rīga} */
    public void respond(String pathAndQuery, String body) {
        responses.put(pathAndQuery, body);
    }

    List<String> requests() {
        return requests;
    }

    public Lvgmc client() {
        InetSocketAddress address = server.getAddress();
        return new Lvgmc("http://" + address.getHostString() + ":" + address.getPort() + "/data");
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private void handle(HttpExchange exchange) throws IOException {
        URI uri = exchange.getRequestURI();
        String key = uri.getPath().substring("/data/".length()) + (uri.getQuery() == null ? "" : "?" + uri.getQuery());
        requests.add(key);
        String body = responses.get(key);
        if (body == null) {
            exchange.sendResponseHeaders(404, -1);
        } else {
            byte[] bytes = body.getBytes(UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
        }
        exchange.close();
    }
}
