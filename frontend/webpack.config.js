const path = require('path');
const CircularDependencyPlugin = require('circular-dependency-plugin');
const CleanWebpackPlugin = require('clean-webpack-plugin');
const CleanCSSPlugin = require('less-plugin-clean-css');
const MiniCssExtractPlugin = require("mini-css-extract-plugin");

module.exports = [{
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
		new CleanWebpackPlugin(['dist/bundle.js'])
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
