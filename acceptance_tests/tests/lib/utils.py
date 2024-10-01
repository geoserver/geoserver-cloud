from pathlib import Path


def write_actual_image(response, tag):
    file = Path(f"/tmp/{tag}_actual.png")
    file.parent.mkdir(parents=True, exist_ok=True)
    with open(file, "wb") as fs:
        fs.write(response.read())


def compare_images(dir, tag):
    actual = f"/tmp/{tag}_actual.png"
    expected = f"{dir}/{tag}_expected.png"
    with open(actual, "rb") as fs1, open(expected, "rb") as fs2:
        assert fs1.read() == fs2.read()
