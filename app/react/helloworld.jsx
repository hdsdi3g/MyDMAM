$(document).ready(function() {
	var node = $("#example")[0];

	React.render(
		<h1>Hello, {i18n("maingrid.brand")} !</h1>,
		node
	);

	mydmam.async.request("demosasync", "get", {somecontent: "Hello!!", vals: [{aa: "FooBar"}]}, function(data) {
		console.log(data);
	});
});
