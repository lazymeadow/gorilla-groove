const path = require('path');
const CleanWebpackPlugin = require('clean-webpack-plugin');
const CleanCSSPlugin = require('less-plugin-clean-css');
const MiniCssExtractPlugin = require("mini-css-extract-plugin");
const webpack = require("webpack");

const appVersion = require('child_process')
	.execSync('node ../backend/get-version.js')
	.toString()
	.trim();

module.exports = (env, argv) => {
	// Ended up not using this, but might be handy so I'm leaving it here
	// noinspection JSUnusedLocalSymbols
	const isProduction = argv.mode === 'production';

	return [{
		entry: './src/index.js',
		output: {
			path: path.resolve('dist'),
			filename: 'bundle.js'
		},
		module: {
			rules: [
				{
					test: /\.js$/,
					exclude: /node_modules/,
					use: "babel-loader"
				}, {
					test: /\.jsx?$/,
					exclude: /node_modules/,
					use: "babel-loader"
				}
			]
		},
		plugins: [
			new CleanWebpackPlugin(['dist/bundle.js']),
			new webpack.DefinePlugin({
				__VERSION__: JSON.stringify(appVersion)
			})
		]

	}, {
		mode: "development",
		entry: {
			index: './src/index.less'
		},
		optimization: {
			splitChunks: {
				cacheGroups: {
					styles: {
						name: 'styles',
						test: /\.css$/,
						chunks: 'all',
						enforce: true
					}
				}
			}
		},
		module: {
			rules: [
				{
					test: /\.less$/,
					use: [
						{
							loader: MiniCssExtractPlugin.loader
						},
						{
							loader: "css-loader"
						},
						{
							loader: "less-loader", options: {
								paths: [
									path.resolve(__dirname, "src")
								],
								plugins: [
									new CleanCSSPlugin({advanced: true})
								]
							}
						}
					]
				}
			]
		},
		plugins: [
			new CleanWebpackPlugin(['dist/index.css', 'dist/index.js']),
			new MiniCssExtractPlugin({
				filename: "[name].css"
			})
		]
	}
	];
};
