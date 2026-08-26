// Add COOP/COEP headers to webpack dev server for SharedArrayBuffer support
// Required by Compose Multiplatform WasmJS
config.devServer = config.devServer || {};
config.devServer.setupMiddlewares = function(middlewares, devServer) {
    devServer.app.use(function(req, res, next) {
        res.setHeader('Cross-Origin-Embedder-Policy', 'credentialless');
        res.setHeader('Cross-Origin-Opener-Policy', 'same-origin');
        next();
    });
    return middlewares;
};
