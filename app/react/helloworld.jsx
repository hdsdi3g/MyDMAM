$(document).ready(function() {
	var node = $("#example")[0];

	React.render(
		<h1>Hello, {i18n("maingrid.brand")} !</h1>,
		node
	);
});