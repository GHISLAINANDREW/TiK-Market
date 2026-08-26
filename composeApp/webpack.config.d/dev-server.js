const path = require('path');

if (config.devServer) {
    // 1. History Fallback
    config.devServer.historyApiFallback = true;

    // 2. Static Assets
    const resourcesPath = path.resolve(__dirname, '../../../../src/wasmJsMain/resources');
    config.devServer.static = config.devServer.static || [];
    config.devServer.static.push(resourcesPath);

    // 3. Headers (COOP/COEP for Wasm SharedArrayBuffer)
    config.devServer.headers = config.devServer.headers || {};
    config.devServer.headers['Cross-Origin-Embedder-Policy'] = 'credentialless';
    config.devServer.headers['Cross-Origin-Opener-Policy'] = 'same-origin';

    // 4. Proxy (Redirect /api requests to Render)
    config.devServer.proxy = [
        {
            context: ['/api'],
            target: 'https://tik-market.onrender.com',
            changeOrigin: true,
            secure: true,
            pathRewrite: { '^/api': '' },
            onProxyReq: function(proxyReq) {
                proxyReq.setHeader('Host', 'tik-market.onrender.com');
            }
        }
    ];
}

// 5. Template Override: use custom index.html from resources
config.plugins.forEach(function(plugin) {
    if (plugin.constructor.name === 'HtmlWebpackPlugin') {
        const templatePath = path.resolve(__dirname, '../../../../src/wasmJsMain/resources/index.html');
        plugin.userOptions.template = templatePath;
    }
});
