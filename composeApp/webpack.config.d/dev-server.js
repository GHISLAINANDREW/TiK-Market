// Webpack dev server configuration for Compose WasmJS
// COOP/COEP headers + API proxy + SPA fallback

// Ensure devServer object exists
if (!config.devServer) {
    config.devServer = {};
}

// 1. SPA fallback: serve index.html for unknown routes
config.devServer.historyApiFallback = true;

// 2. Static assets directory (for index.html template)
var path = require('path');
var resourcesPath = path.resolve(__dirname, '../../../../src/wasmJsMain/resources');
if (!config.devServer.static) {
    config.devServer.static = [];
}
config.devServer.static.push(resourcesPath);

// 3. COOP/COEP headers for SharedArrayBuffer (required by Compose WasmJS)
var prevSetup = config.devServer.setupMiddlewares;
config.devServer.setupMiddlewares = function(middlewares, devServer) {
    if (prevSetup) {
        middlewares = prevSetup(middlewares, devServer);
    }
    devServer.app.use(function(req, res, next) {
        res.setHeader('Cross-Origin-Embedder-Policy', 'credentialless');
        res.setHeader('Cross-Origin-Opener-Policy', 'same-origin');
        next();
    });
    return middlewares;
};

// 4. API proxy: /api/* -> Render backend (strip /api/ like Vercel rewrites)
config.devServer.proxy = [
    {
        context: ['/api'],
        target: 'https://tik-market.onrender.com',
        changeOrigin: true,
        secure: true,
        pathRewrite: { '^/api': '' },
        onProxyReq: function(proxyReq) {
            proxyReq.setHeader('Host', 'tik-market.onrender.com');
        },
        onProxyRes: function(proxyRes) {
            proxyRes.headers['access-control-allow-origin'] = '*';
        }
    }
];

// 5. Template override: use our custom index.html
config.plugins.forEach(function(plugin) {
    if (plugin.constructor.name === 'HtmlWebpackPlugin') {
        plugin.userOptions.template = path.resolve(__dirname, '../../../../src/wasmJsMain/resources/index.html');
    }
});
