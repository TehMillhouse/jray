import java.awt.image.BufferedImage;
import java.util.Optional;
import javax.imageio.ImageIO;
import java.lang.*;
import java.io.*;

class Ray {
	public static void main(String[] argv) {
		int w, h;
		if (argv.length < 2) {
			w = 512;
			h = 512;
		} else {
			w = Integer.parseInt(argv[0]);
			h = Integer.parseInt(argv[1]);
		}
		Ray tracer = new Ray(w, h);
		tracer.render();
		tracer.dump("./spheres.png");

		if (w < 100 && h < 100) {
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					System.out.print((tracer.img.getRGB(x, y) & 0xffffff) != 0 ? '#' : ' ');
				}
				System.out.println();
			}
		} else {
			System.out.println("Dimensions > 100x100, not printing to terminal");
		}
	}

	class Point3d {
		public double x, y, z;
		public Point3d(double x, double y, double z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}

		public double dot(Point3d that) {
			return this.x * that.x + this.y * that.y + this.z * that.z;
		}

		public Point3d clone() {
			return new Point3d(this.x, this.y, this.z);
		}

		public void plus(Point3d that) {
			this.x += that.x;
			this.y += that.y;
			this.z += that.z;
		}

	}

	class Vector3d extends Point3d {
		public Vector3d(double x, double y, double z) {
			super(x, y, z);
		}
		public Vector3d clone() {
			return new Vector3d(this.x, this.y, this.z);
		}

		public double length() {
			return Math.sqrt(x*x + y*y + z*z);
		}

		public void normalize() {
			double len = this.length();
			this.x /= len;
			this.y /= len;
			this.z /= len;
		}

		public Vector3d cross(Vector3d that) {
			return new Vector3d(this.y * that.z - this.z * that.y,
				                this.z * that.x - this.x * that.z,
				                this.x * that.y - this.y * that.x);
		}

		public void scale(double r) {
			this.x *= r;
			this.y *= r;
			this.z *= r;
		}

		public String toString() {
			return "(" + Double.toString(this.x) + ", " + Double.toString(this.y) + ", " + Double.toString(this.z) + ")";
		}
	}

	class Sphere {
		public int color;
		public Point3d coords;
		public double radius;
		public Sphere(Point3d coords, double radius, int color) {
			this.coords = coords;
			this.color = color;
			this.radius = radius;
		}

		public Optional<Double> intersectRay(Point3d origin, Vector3d direction) {
			// direction must already be normalized
			
			Point3d L = new Point3d(origin.x - coords.x,
				                    origin.y - coords.y,
				                    origin.z - coords.z);
			
			double a = direction.dot(direction);
			double b = 2 * direction.dot(L);
			double c = L.dot(L) - this.radius * this.radius;
			double discr = b * b - 4 * a * c;
			if (discr < 0)
				return Optional.empty();
			double x1 = -b - Math.sqrt(discr);
			double x2 = -b + Math.sqrt(discr);
			if (x1 < x2)
				return Optional.of(new Double(x1));
			return Optional.of(new Double(x2));
		}
	}

	BufferedImage img;
	private int width;
	private int height;
	private Sphere[] scene = {
		new Sphere(new Point3d(0.f, 0.f, 0.f), 0.5f, 0xffffff),

		new Sphere(new Point3d(0.5f, 0.f, -0.866f), 0.25f, 0xff0000),
		new Sphere(new Point3d(-0.5f, 0.f, -0.866f), 0.25f, 0xff00ff),
		new Sphere(new Point3d(-1.f, 0.f, 0.f), 0.25f, 0x00ff00),
		new Sphere(new Point3d(-0.5f, 0.f, 0.866f), 0.25f, 0xcccccc),
		new Sphere(new Point3d(0.5f, 0.f, 0.866f), 0.25f, 0x00ffff),
		new Sphere(new Point3d(1.f, 0.f, 0.f), 0.25f, 0x0000ff),
	};


	public Ray(int width, int height) {
		this.img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		this.width = width;
		this.height = height;
	}

	public void dump(String filePath) {
		try {
			File out = new File(filePath);
			ImageIO.write(this.img, "png", out);
		} catch (IOException e) {
			System.err.println("Couldn't save file");
		}
	}

	public Vector3d genRay(Point3d origin, double framesize, int x, int y, Vector3d lookDirection, double focalLength) {
		// assumes lookDirection is normalized
		Vector3d planarLook = new Vector3d(lookDirection.x, 0.f, lookDirection.z);
		if (lookDirection.y == 0.f) {
			// The look vector is already planar, so We need a different vector...
			planarLook.y += 1.f;
		}

		Vector3d xVec = lookDirection.cross(planarLook);
		Vector3d yVec = lookDirection.cross(xVec);

		xVec.normalize();
		yVec.normalize();

		Vector3d screenOrigin = new Vector3d(origin.x, origin.y, origin.z);
		Vector3d tmp = lookDirection.clone();
		tmp.scale(focalLength);
		screenOrigin.plus(tmp);
		tmp = xVec.clone();
		tmp.scale(framesize / -2.f);
		screenOrigin.plus(tmp);
		double frameheight = ((double) this.height / (double) this.width) * framesize;
		double xstep = framesize / this.width;
		double ystep = frameheight / this.height;
		tmp = yVec.clone();
		tmp.scale(frameheight / -2.f);
		screenOrigin.plus(tmp);
		xVec.scale(x * xstep);
		yVec.scale(y * ystep);

		screenOrigin.plus(xVec);
		screenOrigin.plus(yVec);
		
		screenOrigin.x -= origin.x;
		screenOrigin.y -= origin.y;
		screenOrigin.z -= origin.z;
		screenOrigin.normalize();
		return screenOrigin;
	}

	public void render() {

		double focalLength = 2.1f;
		Vector3d lookDir = new Vector3d(0.f, 0.35, 1);
		lookDir.normalize();
		Point3d origin = new Point3d(0, -0.2f, -0.2f);
		Vector3d tmp = lookDir.clone();
		tmp.scale(-focalLength);
		origin.plus(tmp);
		double framewidth = 5.f;

		for (int y = 0; y < this.height; y++) {
			for(int x = 0; x < this.width; x++) {
				double tmin = Double.MAX_VALUE;
				for (Sphere sphere : scene) {
					Vector3d dir = genRay(origin, framewidth, x, y, lookDir, focalLength);
					Optional<Double> t = sphere.intersectRay(origin, dir);
					if (t.isPresent() && t.get().doubleValue() < tmin) {
						tmin = t.get().doubleValue();
						this.img.setRGB(x, y, sphere.color);
					}
				}
			}
		}
	}
}
// vim: set noet: set tabstop=4:
