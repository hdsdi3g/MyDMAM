/*
 * This file is part of MyDMAM.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * Copyright (C) hdsdi3g for hd3g.tv 2015
 * 
 * Inspired from React Tutorial: http://facebook.github.io/react/docs/tutorial.html
 * 
*/

$(document).ready(function() {
	if ($("#example").length === 0) {
		return;	
	}

	var Comment = React.createClass({
		handleDelete: function(e) {
			e.preventDefault();
			this.props.doaction.onCommentDelete(this.props.id);
			//this.props.doaction.hw(this.props.id);
			return;
		},
		handleEdit: function(e) {
			e.preventDefault();
			this.props.doaction.onCommentEdit(this.props.id);
			return;
		},
		render: function() {
			return (
				<div className="comment">
					<a href="" className="btn" onClick={this.handleDelete}>Delete</a>&nbsp;
					<a href="" className="btn" onClick={this.handleEdit}>Edit</a>&nbsp;
					<strong>{this.props.author}</strong> &bull;
					{this.props.children}
				</div>
			);
		}
	});

	var CommentList = React.createClass({
		render: function() {
			var re_wrap_doaction = {
				doaction: this.props.doaction,
			};
			var commentNodes = this.props.data.map(function (comment) {
				return (
					<Comment {...re_wrap_doaction} key={comment.key} author={comment.author} id={comment.key}>
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
		handleChange: function(event) {
			var author = React.findDOMNode(this.refs.author).value;
		    var text = React.findDOMNode(this.refs.text).value;
		    var key = this.props.edit.key;
		    var edit = {key: key, author: author, text: text};
			this.props.onChangeComment(edit);
		},
		handleSubmit: function(e) {
			e.preventDefault();
			var author = React.findDOMNode(this.refs.author).value.trim();
		    var text = React.findDOMNode(this.refs.text).value.trim();
		    if (!text || !author) {
				return;
			}

			this.props.onCommentSubmit({text: text, author: author, key: this.props.edit.key});
			React.findDOMNode(this.refs.author).value = '';
		    React.findDOMNode(this.refs.text).value = '';

		    return;
		},
		render: function() {
			//console.log(this.props);
			return (
				<form className="commentForm" onSubmit={this.handleSubmit} editkey={this.props.edit.key}>
					<input type="text" placeholder="Your name" ref="author" value={this.props.edit.author} onChange={this.handleChange} />
					<input type="text" placeholder="Say something..." ref="text" value={this.props.edit.text} onChange={this.handleChange} />
					<input type="submit" value="Post" className="btn" />
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
		handleCommentSubmit: function(comment) {
			var actual_comments = this.state.data;
			var cleared_edit = {key: null, author: null, text: null};
			if (comment.key) {
				/** edit comment */
				for (var i = 0; i < actual_comments.length; i++) {
					if (comment.key === actual_comments[i].key) {
						actual_comments[i] = comment;
						break;
					}
				}
				this.setState({data: actual_comments, edit: cleared_edit});
				mydmam.async.request("demosasync", "edit", comment, function(data) {
					this.setState({data: data.commentlist});
				}.bind(this));
			} else {
				/** add comment */
				var new_comments = actual_comments.concat([comment]);
				/** Sans key, mais c'est pas grave, car il dégagera/recyclera au prochain async.request */
				this.setState({data: new_comments, edit: cleared_edit});
				mydmam.async.request("demosasync", "add", comment, function(data) {
					this.setState({data: data.commentlist});
				}.bind(this));
			}
		},
		handleCommentDelete: function(comment_key) {
			var comment;
			var newComments = [];
			for (var i = 0; i < this.state.data.length; i++) {
				comment = this.state.data[i]
				if (comment.key !== comment_key) {
					newComments.push(comment);
				}
			};
			this.setState({data: newComments});
			mydmam.async.request("demosasync", "del", {key : comment_key}, function(data) {
				this.setState({data: data.commentlist});
			}.bind(this));
		},
		handleCommentEdit: function(comment_key) {
			var comment;
			for (var i = 0; i < this.state.data.length; i++) {
				if (comment_key === this.state.data[i].key) {
					comment = this.state.data[i];
					break;
				}
			};
			this.setState({edit: comment})
		},
		handleChangeComment: function(comment) {
			var new_comment_list = this.state.data;
			for (var i = 0; i < new_comment_list.length; i++) {
				if (comment.key === new_comment_list[i].key) {
					new_comment_list[i] = comment;
					break;
				}
			};

			this.setState({data: new_comment_list, edit: comment});
		},
		getInitialState: function() {
			return {data: [], edit: {key: null, author: null, text: null}};
		},
		componentDidMount: function() {
			this.loadCommentsFromServer();
			setInterval(this.loadCommentsFromServer, this.props.pollInterval);
		},
		render: function() {
			var action_comment_list = {
				doaction: {
					onCommentDelete: this.handleCommentDelete,
					hw: function(key) {
						console.log("HELLO!", key);
					},
					onCommentEdit: this.handleCommentEdit,
				},
			};

		    return (
				<div className="commentBox">
			        <h1>Comments</h1>
			        <CommentList data={this.state.data} {...action_comment_list} />
					<CommentForm onCommentSubmit={this.handleCommentSubmit} edit={this.state.edit} onChangeComment={this.handleChangeComment} />
				</div>
			);
		}
	});

	React.render(
		<CommentBox pollInterval={200000} />,
		$("#example")[0]
	);

});
