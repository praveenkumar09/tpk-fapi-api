function fn() {
    var port = java.lang.System.getProperty('quarkus.http.test-port') || '8081';
    return {
        baseUrl: 'http://localhost:' + port + '/api'
    };
}