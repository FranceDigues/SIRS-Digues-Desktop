import json
import math


def compute_color(i, a, b, c):
    if i <= a:
        index = i
        size = a
    elif i <= b:
        index = i - (a + 1)
        size = b - a
    elif i <= c:
        index = i - (b + 1)
        size = c - b
    else:
        index = i - (c + 1)
        size = c - b

    third = size / 3
    bound1, bound2 = round(third), round(third * 2)
    if index <= bound1:
        a = math.floor(256 * (index / bound1))
        b = math.floor(256 * ((bound1 - index) / bound1))
        c = 255
    elif index <= bound2:
        index = index - (bound1 + 1)
        difference = bound2 - bound1
        a = 255
        b = math.floor(256 * (index / difference))
        c = math.floor(256 * ((difference - index) / difference))
    else:
        index = index - (bound2 + 1)
        difference = bound2 - bound1
        a = math.floor(256 * ((difference - index) / difference))
        b = 255
        c = math.floor(256 * (index / difference))
    ha = hex(a).split('x')[1]
    hb = hex(b).split('x')[1]
    hc = hex(c).split('x')[1]
    return '#' + ha + hb + hc


def compute_boundary(size):
    quarter = size / 4
    return round(quarter), round(quarter * 2), round(quarter * 3)


def compute_style(size, index):
    a, b, c = compute_boundary(size)
    color = compute_color(index, a, b, c)

    if index <= a:
        symbol = "triangle"
        capstyle = "flat"  # flat, square, round
        line_style = "solid"  # solid, dash (or dashed)
        joinstyle = "miter"  # miter, bevel, round
    elif index <= b:
        symbol = "square"
        capstyle = "square"
        line_style = "solid"
        joinstyle = "bevel"
    elif index <= c:
        symbol = "circle"
        capstyle = "round"
        line_style = "solid"
        joinstyle = "round"
    else:
        symbol = "cross"
        capstyle = "square"
        line_style = "dash"
        joinstyle = "bevel"
    return {
        "point": {
            "name": symbol,
            "color": color
        },
        "line": {
            "color": color,
            "width": "1.0",
            "capstyle": capstyle,
            "line_style": line_style,
            "joinstyle": joinstyle
        },
        "polygon": {
            "color": color,
            "outline_color": "black"
        }
    }


def update_style_from_data(data):
    size = len(data)
    i = 0

    for clazzStr in data:
        data[clazzStr]["style"] = compute_style(size, i)
        data[clazzStr]["crs"] = "EPSG:2154"
        i += 1


def update_style():
    CONFIGURATION_LABEL_FILE = "qgis-plugin-couchdb/couchdb_importer/user_preference_correspondence.json"
    with open(CONFIGURATION_LABEL_FILE) as f:
        data = json.load(f)
    update_style_from_data(data)
    with open(CONFIGURATION_LABEL_FILE, 'w') as f:
        json.dump(data, f)


if __name__ == "__main__":
    update_style()
