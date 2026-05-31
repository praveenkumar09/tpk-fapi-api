function fn() {
    var port = karate.properties['quarkus.http.test-port'] || '8083';
    var config = {
        baseUrl: 'http://localhost:' + port + '/api'
    };

    karate.configure('connectTimeout', 10000);
    karate.configure('readTimeout', 10000);
    karate.configure('logPrettyRequest', true);
    karate.configure('logPrettyResponse', true);

    return config;
}