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
 * Copyright (C) hdsdi3g for hd3g.tv 2017
 * 
 * Look and feel GUI API/Toolbox
 */

var styles = null;

lookandfeel.getBaseLuma = function() {
	return mydmam.user.getPreferenceOrDefault("lookandfeel.luma", 100, false); // white
};

lookandfeel.getBaseHue = function() {
	return mydmam.user.getPreferenceOrDefault("lookandfeel.hue", 230, false); // blue
};

lookandfeel.applyStyle = function(item_type, item_style) {
	if (styles == null) {
		var base_luma = lookandfeel.getBaseLuma();
		var base_hue = lookandfeel.getBaseHue();

		styles = {
			base: {
				luma: base_luma,
				hue: base_hue,
			},
			default: {},
			well: {},
			block: {},
			link_normal: {},
			link_hover: {},
			btn_normal: {},
			btn_hover: {},
			btn_active: {},
			btn_well_normal: {},
			btn_well_hover: {},
			btn_well_active: {},
			btn_block_normal: {},
			btn_block_hover: {},
			btn_block_active: {},
		};

		/**
		 * Default color
		 */
		var base_background_luma = (base_luma * 0.35) + 14;
		var base_text_luma = (base_luma * 0.35) + 80;
		if (base_luma > 50) {
			base_background_luma = (base_luma * 0.7) + 30;
			base_text_luma = (base_luma * 0.1) + 5;
		}
		styles.default = {
			backgroundColor: cssHSL(0, 0, base_background_luma),
			color: cssHSL(0, 0, base_text_luma),
		}

		/**
		 * Well color
		 */
 		var well_background_luma = base_background_luma + 10;
		var well_background_sat = 30;
		if (base_luma > 50) {
			well_background_luma = base_background_luma - 8;
			well_background_sat = 30;
		}
		styles.well = {
			backgroundColor: cssHSL(base_hue, well_background_sat, well_background_luma),
			color: cssHSL(0, 0, base_text_luma),
			border: "1px solid " + cssHSL(base_hue, well_background_sat, well_background_luma - 8),
		};

		/**
		 * Link
		 */ 
		var link_color = cssHSL(base_hue + 10, 50, 60);
		if (base_luma > 25) {
			link_color = cssHSL(base_hue + 10, 70, 75);
		}
		if (base_luma > 50) {
			/** mid */
			if (base_hue > 20 && base_hue < 200) {
				/* orange -> blue */
				link_color = cssHSL(base_hue + 10, 70, 20);
			} else {
				/* red -> orange / blue -> violet */
				link_color = cssHSL(base_hue + 10, 80, 15);
			}
		}
		if (base_luma > 75) {
			/** high */
			link_color = cssHSL(base_hue + 10, 100, 30);
		}

		styles.link_normal = {
			color: link_color,
			textDecorationColor: link_color,
			textDecorationStyle: "dotted",
			textDecorationLine: "underline",
		};

		styles.link_hover = {
			color: link_color,
			textDecorationColor: link_color,
			textDecorationStyle: "solid",
			textDecorationLine: "underline",
		};

		/**
		 * Block
		 */
		var block_color = base_background_luma + 5;
		if (base_luma > 50) {
			block_color = base_background_luma - 5;
			if (base_luma > 90) {
				block_color = base_background_luma - 8;
			}
		}
		styles.block = {
			backgroundColor: cssHSL(0, 0, block_color),
		};

		/**
		 * Buttons
		 */
		var all_buttons = {
			textDecorationLine: "none",
			borderRadius: "5px",
		};

		/**
		 * Button normal > default/well/block
		 */
		styles.btn_normal = Object.assign({
			color: cssHSL(0, 0, base_text_luma),
			backgroundColor: cssHSL(0, 0, base_background_luma),
			border: "1px solid " + cssHSL(base_hue, 60, base_luma > 50 ? 40 : 70, 0.5),
		}, all_buttons);

		styles.btn_well_normal = Object.assign({
			color: cssHSL(0, 0, base_text_luma),
			border: "1px solid " + cssHSL(base_hue, 60, base_luma > 50 ? 40 : 70, 0.5),
			textDecorationLine: "none",
			borderRadius: "5px",
		}, all_buttons);
		styles.btn_block_normal = Object.assign({
			color: cssHSL(0, 0, base_text_luma),
			border: "1px solid " + cssHSL(base_hue, 60, base_luma > 50 ? 40 : 70, 0.5),
			textDecorationLine: "none",
			borderRadius: "5px",
		}, all_buttons);

		/**
		 * Button hover > default/well/block
		 */
		styles.btn_hover = Object.assign({
			color: cssHSL(0, 0, base_text_luma),
			backgroundColor: cssHSL(0, 0, block_color),
			border: "1px solid " + cssHSL(base_hue, 50, base_luma > 50 ? 30 : 60, 0.5),
			borderRadius: "5px",
			textDecorationLine: "none",
		}, all_buttons);
		styles.btn_well_hover = Object.assign({
			color: cssHSL(0, 0, base_text_luma),
			backgroundColor: cssHSL(base_hue, well_background_sat, well_background_luma - 3),
			border: "1px solid " + cssHSL(base_hue, 50, base_luma > 50 ? 30 : 60, 0.5),
			borderRadius: "5px",
			textDecorationLine: "none",
		}, all_buttons);
		styles.btn_block_hover = Object.assign({
			color: cssHSL(0, 0, base_text_luma),
			backgroundColor: cssHSL(0, 0, block_color - 5),
			border: "1px solid " + cssHSL(base_hue, 50, base_luma > 50 ? 30 : 60, 0.5),
			borderRadius: "5px",
			textDecorationLine: "none",
		}, all_buttons);

		/**
		 * Button active > default/well/block
		 */
		var createGrad = function(source_dark, source_light, dest) {
			var result = cssGrad("to bottom", [
				{c: source_dark, step: 0},
				{c: dest, step: 40},
				{c: dest, step: 100},
			]);
			if (base_luma > 50) {
				result = cssGrad("to bottom", [
					{c: source_light, step: 0},
					{c: dest, step: 50},
					{c: dest, step: 100},
				]);
			}
			return result;
		};

		styles.btn_active = Object.assign({
			color: cssHSL(0, 0, base_text_luma),
			background: createGrad(cssHSL(base_hue, well_background_sat - 20, well_background_luma - 15), cssHSL(base_hue, well_background_sat - 50, well_background_luma - 10), styles.btn_hover.backgroundColor),
			border: "1px solid " + cssHSL(base_hue, 50, base_luma > 50 ? 30 : 60, 0.5),
			borderRadius: "5px",
			textDecorationLine: "none",
		}, all_buttons);
		styles.btn_well_active = Object.assign({
			color: cssHSL(0, 0, base_text_luma),
			background: createGrad(cssHSL(base_hue, well_background_sat - 20, well_background_luma - 15), cssHSL(base_hue, well_background_sat - 50, well_background_luma - 10), styles.btn_hover.backgroundColor),
			border: "1px solid " + cssHSL(base_hue, 50, base_luma > 50 ? 30 : 60, 0.5),
			borderRadius: "5px",
			textDecorationLine: "none",
		}, all_buttons);
		styles.btn_block_active = Object.assign({
			color: cssHSL(0, 0, base_text_luma),
			background: createGrad(cssHSL(base_hue, well_background_sat - 20, well_background_luma - 15), cssHSL(base_hue, well_background_sat - 50, well_background_luma - 5), styles.btn_hover.backgroundColor),
			border: "1px solid " + cssHSL(base_hue, 50, base_luma > 50 ? 30 : 60, 0.5),
			borderRadius: "5px",
			textDecorationLine: "none",
		}, all_buttons);
	}

	if (! styles[item_type]){
		console.error("Can't found type " + item_type);
	} else {
		if (! item_style) {
			item_style = {};
		}
		var style_type = styles[item_type];
		for (var css_key in style_type) {
			item_style[css_key] = style_type[css_key];
		}
	}
	return item_style;
};

lookandfeel.changeBaseColors = function(base_luma, base_hue) {
	styles = null;
	mydmam.user.setPreference("lookandfeel.luma", base_luma, true);
	mydmam.user.setPreference("lookandfeel.hue", base_hue, true);
	mydmam.user.delayedPushToServer();
};
