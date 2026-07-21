// Override HtmlWebpackPlugin to use the custom index.html from resources
config.plugins.forEach(function(plugin) {
    if (plugin.constructor.name === 'HtmlWebpackPlugin') {
        plugin.userOptions.template = __dirname + '/../src/wasmJsMain/resources/index.html';
    }
});
