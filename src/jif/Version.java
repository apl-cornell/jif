package jif;

/**
 * Jif version information.
 *
 * DO NOT EDIT THIS CLASS BY HAND!  This is automatically generated by Ant.  If
 * you wish to change the version number, run one of the following:
 *
 *   ant bump-version  (Bumps the version number.)
 *   ant bump-major    (Bumps the major version number.)
 *   ant bump-minor    (Bumps the minor version number.)
 *   ant bump-patch    (Bumps the patch level.)
 */
public class Version extends polyglot.main.Version {
  @Override
  public String name() {
    return "jif";
  }

  @Override
  public int major() {
    return 3;
  }

  @Override
  public int minor() {
    return 5;
  }

  @Override
  public int patch_level() {
    return 0;
  }

  @Override
  public String toString() {
    return "3.5.0 (2013-04-17 13:29:51 EDT)";
  }
}
