search.SearchForm = React.createClass({
	getInitialState: function() {
		return {new_q: ""};
	},
	componentDidMount: function() {
		this.setState({new_q: this.props.results.q});
		React.findDOMNode(this.refs.q).focus();
	},
	handleChange: function(event) {
		this.setState({new_q: React.findDOMNode(this.refs.q).value});
	},
	handleSubmit: function(e) {
		e.preventDefault();
		var q = React.findDOMNode(this.refs.q).value.trim();
	    if (!q) {
			return;
		}
		this.props.onSearchFormSubmit({q: q});
	},
	render: function() {
	    return (
	    	<form className="search-query form-search" onSubmit={this.handleSubmit}>
				<div className="input-append">
					<input type="text" ref="q" value={this.state.new_q} placeholder={i18n("maingrid.search")} className="search-query span10" onChange={this.handleChange} />
					<button className="btn btn-info" type="submit">{i18n("maingrid.search")}</button>
				</div>
			</form>
		);
	}
});