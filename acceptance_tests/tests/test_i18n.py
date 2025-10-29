#!/bin/env python

import pytest
from tests.conftest import RESOURCE_DIR
from tests.lib.utils import compare_images, write_actual_image
from requests.exceptions import JSONDecodeError
from sqlalchemy.sql import text

WORKSPACE = "test_i18n_workspace"


def international_title(default=True, de=True, fr=True, it=True, rm=True):
    title = {}
    if default:
        title["default"] = "Default title"
    if de:
        title["de"] = "Punkte"
    if fr:
        title["fr"] = "Points"
    if it:
        title["it"] = "Punti"
    if rm:
        title["rm"] = "Puncts"
    return title


def assert_legend(geoserver, style, language, expected_label):
    response = geoserver.get_legend_graphic(
        "i18n_legend",
        format="application/json",
        language=language,
        style=style,
        workspace_name=WORKSPACE,
    )
    try:
        label = response.json()["Legend"][0]["rules"][0]["title"]
        assert label == expected_label
    except (KeyError, JSONDecodeError):
        print(f"Invalid response for language '{language}:'\n{response.content}")
        assert False


@pytest.fixture(scope="module")
def geoserver(geoserver_factory):
    geoserver = geoserver_factory(WORKSPACE)
    geoserver.create_pg_datastore(
        workspace_name=WORKSPACE,
        datastore_name="i18n_datastore",
        pg_host="geodatabase",
        pg_port=5432,
        pg_db="acceptance",
        pg_user="geoserver",
        pg_password="geoserver",
        pg_schema="test1",
        set_default_datastore=True,
    )
    yield geoserver


@pytest.fixture(scope="module")
def geoserver_with_i18n_layers(geoserver):

    # Create feature type with all languages
    layer1 = "layer_all_languages"
    title1 = international_title(default=True, de=True, fr=True, it=True, rm=True)
    geoserver.create_feature_type(layer1, title=title1, epsg=2056)

    # Create feature type without Rumantsch
    layer2 = "layer_no_rumantsch"
    title2 = international_title(default=True, de=True, fr=True, it=True, rm=False)
    geoserver.create_feature_type(layer2, title=title2, epsg=2056)

    # Create feature type without default language nor Rumantsch
    layer3 = "layer_no_default_no_rumantsch"
    title3 = international_title(default=False, de=True, fr=True, it=True, rm=False)
    geoserver.create_feature_type(layer3, title=title3, epsg=2056)

    yield geoserver


@pytest.fixture(scope="module")
def geoserver_default_locale_it(geoserver_with_i18n_layers):
    geoserver_with_i18n_layers.set_default_locale_for_service(WORKSPACE, "it")
    yield geoserver_with_i18n_layers
    geoserver_with_i18n_layers.unset_default_locale_for_service(WORKSPACE)


@pytest.fixture(scope="module")
def geoserver_i18n_legend_layer(geoserver):
    geoserver.create_feature_type("i18n_legend", epsg=2056)
    geoserver.create_style_from_file(
        "localized_with_default",
        f"{RESOURCE_DIR}/localized_with_default.sld",
        workspace_name=WORKSPACE,
    )
    geoserver.create_style_from_file(
        "localized_no_default",
        f"{RESOURCE_DIR}/localized_no_default.sld",
        workspace_name=WORKSPACE,
    )
    yield geoserver


@pytest.fixture(scope="function")
def geoserver_i18n_legend_layer_and_default_locale_it(geoserver_i18n_legend_layer):
    geoserver_i18n_legend_layer.set_default_locale_for_service(WORKSPACE, "it")
    yield geoserver_i18n_legend_layer
    geoserver_i18n_legend_layer.unset_default_locale_for_service(WORKSPACE)


@pytest.fixture(scope="module")
def geoserver_i18n_label_layer(geoserver, db_session):
    feature_type = "i18n_labels"
    style = "localized_labels"
    file = f"{RESOURCE_DIR}/{style}.sld"
    attributes = {
        "geom": {"type": "Point", "required": True},
        "label_default": {"type": "string", "required": False},
        "label_de": {"type": "string", "required": False},
        "label_fr": {"type": "string", "required": False},
    }
    geoserver.create_feature_type(feature_type, attributes=attributes, epsg=2056)
    geoserver.create_style_from_file(style, file, workspace_name=WORKSPACE)
    # Feature with labels in German, French and a default value
    db_session.execute(
        text(
            f"INSERT INTO {feature_type} (geom, label_default, label_de, label_fr) VALUES "
            "(public.ST_SetSRID(public.ST_MakePoint(2600000, 1200000), 2056), 'Default label', 'Deutsches Label', 'Étiquette française')"
        )
    )
    # Feature with labels in German, French and no default value
    db_session.execute(
        text(
            f"INSERT INTO {feature_type} (geom, label_de, label_fr) VALUES "
            "(public.ST_SetSRID(public.ST_MakePoint(2700000, 1300000), 2056), 'Deutsches Label', 'Étiquette française')"
        )
    )
    db_session.commit()
    yield geoserver


@pytest.fixture(scope="module")
def geoserver_i18n_label_default_locale_fr(geoserver_i18n_label_layer):
    geoserver_i18n_label_layer.set_default_locale_for_service(WORKSPACE, "fr")
    yield geoserver_i18n_label_layer
    geoserver_i18n_label_layer.unset_default_locale_for_service(WORKSPACE)


@pytest.mark.parametrize(
    "language,expected_titles",
    [
        (
            "de",
            {
                "layer_all_languages": "Punkte",
                "layer_no_rumantsch": "Punkte",
                "layer_no_default_no_rumantsch": "Punkte",
            },
        ),
        (
            "de,fr",
            {
                "layer_all_languages": "Punkte",
                "layer_no_rumantsch": "Punkte",
                "layer_no_default_no_rumantsch": "Punkte",
            },
        ),
        (
            "fr,de",
            {
                "layer_all_languages": "Points",
                "layer_no_rumantsch": "Points",
                "layer_no_default_no_rumantsch": "Points",
            },
        ),
        (
            "rm",
            {
                "layer_all_languages": "Puncts",
                "layer_no_rumantsch": "Default title",
                "layer_no_default_no_rumantsch": "DID NOT FIND i18n CONTENT FOR THIS ELEMENT",
            },
        ),
        (
            "en",
            {},
        ),
        (
            None,
            {
                "layer_all_languages": "Default title",
                "layer_no_rumantsch": "Default title",
                "layer_no_default_no_rumantsch": "Punkte",
            },
        ),
        (
            "foobar",
            {},
        ),
    ],
)
def test_i18n_layers(geoserver_with_i18n_layers, language, expected_titles):
    capabilities = geoserver_with_i18n_layers.get_wms_layers(WORKSPACE, language)
    layers = capabilities.get("Layer")
    if type(layers) is list:
        for expected_layer, expected_title in expected_titles.items():
            actual_layer = next(
                (layer for layer in layers if layer["Name"] == expected_layer), {}
            )
            assert actual_layer.get("Title") == expected_title
    else:
        print(capabilities)
        assert expected_titles == {}
        assert "ServiceExceptionReport" in capabilities


@pytest.mark.parametrize(
    "language,expected_titles",
    [
        (
            "de",
            {
                "layer_all_languages": "Punkte",
                "layer_no_rumantsch": "Punkte",
                "layer_no_default_no_rumantsch": "Punkte",
            },
        ),
        (
            "rm",
            {
                "layer_all_languages": "Puncts",
                "layer_no_rumantsch": "Default title",
                "layer_no_default_no_rumantsch": "DID NOT FIND i18n CONTENT FOR THIS ELEMENT",
            },
        ),
        (
            "en",
            {},
        ),
        (
            None,
            {
                "layer_all_languages": "Punti",
                "layer_no_rumantsch": "Punti",
                "layer_no_default_no_rumantsch": "Punti",
            },
        ),
    ],
)
@pytest.mark.skip(reason="Default locale is ignored in gs-cloud 1.6.1")
def test_i18n_layers_default_locale(
    geoserver_default_locale_it, language, expected_titles
):
    layers = geoserver_default_locale_it.get_wms_layers(WORKSPACE, language)
    if type(layers) is list:
        for expected_layer, expected_title in expected_titles.items():
            actual_layer = next(
                (layer for layer in layers if layer["Name"] == expected_layer), {}
            )
            print(actual_layer["Name"])
            assert actual_layer.get("Title") == expected_title
    else:
        print(layers)
        assert expected_titles == {}
        assert "ServiceExceptionReport" in layers


@pytest.mark.parametrize(
    "language,expected_label",
    [
        ("en", "English"),
        ("de", "Deutsch"),
        ("fr", "Français"),
        ("it", "Italiano"),
        ("rm", "Default label"),
        (None, "Default label"),
        ("ru", "Default label"),
        ("foobar", "Default label"),
        ("it,fr,de", "Default label"),
    ],
)
def test_i18n_legend_with_default_value(
    geoserver_i18n_legend_layer, language, expected_label
):
    assert_legend(
        geoserver_i18n_legend_layer,
        "localized_with_default",
        language,
        expected_label,
    )


@pytest.mark.parametrize(
    "language,expected_label",
    [
        ("it", "Italiano"),
        ("rm", ""),
        (None, ""),
        ("ru", ""),
        ("foobar", ""),
        ("it,fr,de", ""),
    ],
)
def test_i18n_legend_no_default_value(
    geoserver_i18n_legend_layer, language, expected_label
):

    assert_legend(
        geoserver_i18n_legend_layer,
        "localized_no_default",
        language,
        expected_label,
    )


@pytest.mark.parametrize(
    "language,expected_label",
    [
        ("en", "English"),
        ("de", "Deutsch"),
        ("fr", "Français"),
        ("it", "Italiano"),
        ("rm", "Default label"),
        (None, "Default label"),
        ("ru", "Default label"),
        ("foobar", "Default label"),
        ("it,fr,de", "Default label"),
    ],
)
def test_i18n_legend_with_default_value_and_default_locale(
    geoserver_i18n_legend_layer_and_default_locale_it, language, expected_label
):
    assert_legend(
        geoserver_i18n_legend_layer_and_default_locale_it,
        "localized_with_default",
        language,
        expected_label,
    )


@pytest.mark.parametrize(
    "language,expected_label",
    [
        ("it", "Italiano"),
        ("rm", ""),
        (None, ""),
        ("ru", ""),
        ("foobar", ""),
        ("it,fr,de", ""),
    ],
)
def test_i18n_legend_no_default_value_default_locale(
    geoserver_i18n_legend_layer_and_default_locale_it, language, expected_label
):

    assert_legend(
        geoserver_i18n_legend_layer_and_default_locale_it,
        "localized_no_default",
        language,
        expected_label,
    )


@pytest.mark.parametrize("language", ["de", "fr", "it", None, ""])
def test_i18n_labels(geoserver_i18n_label_layer, language):

    response = geoserver_i18n_label_layer.get_map(
        layers=["i18n_labels"],
        bbox=(2599999.5, 1199999.5, 2600000.5, 1200000.5),
        size=(300, 100),
        format="image/png",
        transparent=False,
        styles=["localized_labels"],
        language=language,
    )

    file_root = f"labels/no_default_locale/default_value/language_{language}"
    write_actual_image(response, file_root)
    compare_images(RESOURCE_DIR, file_root)


@pytest.mark.parametrize("language", ["it", "", None])
def test_i18n_labels_no_default_value(geoserver_i18n_label_layer, language):

    response = geoserver_i18n_label_layer.get_map(
        layers=["i18n_labels"],
        bbox=(2699999.5, 1299999.5, 2700000.5, 1300000.5),
        size=(300, 100),
        format="image/png",
        transparent=False,
        styles=["localized_labels"],
        language=language,
    )

    file_root = f"labels/no_default_locale/no_default_value/language_{language}"
    write_actual_image(response, file_root)
    compare_images(RESOURCE_DIR, file_root)


@pytest.mark.parametrize("language", ["de", "fr", "it", None, ""])
def test_i18n_labels_default_locale(geoserver_i18n_label_default_locale_fr, language):

    response = geoserver_i18n_label_default_locale_fr.get_map(
        layers=["i18n_labels"],
        bbox=(2599999.5, 1199999.5, 2600000.5, 1200000.5),
        size=(300, 100),
        format="image/png",
        transparent=False,
        styles=["localized_labels"],
        language=language,
    )

    file_root = f"labels/default_locale/default_value/language_{language}"
    write_actual_image(response, file_root)
    compare_images(RESOURCE_DIR, file_root)


@pytest.mark.parametrize("language", ["it", "", None])
def test_i18n_labels_no_default_value_default_locale(
    geoserver_i18n_label_default_locale_fr, language
):

    response = geoserver_i18n_label_default_locale_fr.get_map(
        layers=["i18n_labels"],
        bbox=(2699999.5, 1299999.5, 2700000.5, 1300000.5),
        size=(300, 100),
        format="image/png",
        transparent=False,
        styles=["localized_labels"],
        language=language,
    )

    file_root = f"labels/default_locale/no_default_value/language_{language}"
    write_actual_image(response, file_root)
    compare_images(RESOURCE_DIR, file_root)
