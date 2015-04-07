$(document).ready(function() {

	var Comment = React.createClass({
		render: function() {
			return (
				<div className="comment">
					<h3 className="commentAuthor">
						{this.props.author}
					</h3>
					{this.props.children}
				</div>
			);
		}
	});

	var CommentList = React.createClass({
		render: function() {
			var commentNodes = this.props.data.map(function (comment) {
				return (
					<Comment key={comment.key} author={comment.author}>
						{comment.text}
					</Comment>
				);
			});

			return (
				<div className="commentList">
			        {commentNodes}
				</div>
			);
		}
	});

	var CommentForm = React.createClass({
		handleSubmit: function(e) {
			e.preventDefault();
			var author = React.findDOMNode(this.refs.author).value.trim();
		    var text = React.findDOMNode(this.refs.text).value.trim();
		    if (!text || !author) {
				return;
			}

			mydmam.async.request("demosasync", "add", {text: text, author: author}, function(data) {
				//this.setState({data: data.actualcontent})
			}.bind(this));

			React.findDOMNode(this.refs.author).value = '';
		    React.findDOMNode(this.refs.text).value = '';
		    return;
		},
		render: function() {
			return (
				<form className="commentForm" onSubmit={this.handleSubmit}>
					<input type="text" placeholder="Your name" ref="author" />
					<input type="text" placeholder="Say something..." ref="text" />
					<input type="submit" value="Post" />
				</form>
			);
		}
	});

	var CommentBox = React.createClass({
		loadCommentsFromServer: function() {
			mydmam.async.request("demosasync", "get", {}, function(data) {
				this.setState({data: data.commentlist})
			}.bind(this));
		},
		getInitialState: function() {
			return {data: []};
		},
		componentDidMount: function() {
			this.loadCommentsFromServer();
			setInterval(this.loadCommentsFromServer, this.props.pollInterval);
		},
		render: function() {
		    return (
				<div className="commentBox">
			        <h1>Comments</h1>
			        <CommentList data={this.state.data} />
					<CommentForm />
				</div>
			);
		}
	});

	React.render(
		<CommentBox pollInterval={2000} />,
		$("#example")[0]
	);

	/*React.render(
		<h1>Hello, {i18n("maingrid.brand")} !</h1>,
		node
	);*/

});
