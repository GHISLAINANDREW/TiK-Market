const path = require('path');

config.devServer = config.devServer || {};

// 1. History Fallback: fix "Cannot GET /"
config.devServer.historyApiFallback = true;

// 2. Static Assets: ensure index.html and wasm are found
const resourcesPath = path.resolve(__dirname, '../../../../src/wasmJsMain/resources');
config.devServer.static = config.devServer.static || [];
config.devServer.static.push(resourcesPath);

// 3. Template Override: use our custom index.html
config.plugins.forEach(function(plugin) {
    if (plugin.constructor.name === 'HtmlWebpackPlugin') {
        plugin.userOptions.template = path.join(resourcesPath, 'index.html');
    }
});

// 4. Headers: COOP/COEP for SharedArrayBuffer (Wasm requirement)
config.devServer.setupMiddlewares = function(middlewares, devServer) {
    devServer.app.use(function(req, res, next) {
        res.setHeader('Cross-Origin-Embedder-Policy', 'credentialless');
        res.setHeader('Cross-Origin-Opener-Policy', 'same-origin');
        next();
    });
    return middlewares;
};

// 5. Proxy /api/* to Render Backend
config.devServer.proxy = config.devServer.proxy || {};
config.devServer.proxy['/api'] = {
    target: 'https://tik-market.onrender.com',
    changeOrigin: true,
    secure: true,
    onProxyReq: (proxyReq) => {
        proxyReq.setHeader('Host', 'tik-market.onrender.com');
    }
};
