{ pkgs ? import ./pkgs.nix {} }: with pkgs;

stdenv.mkDerivation rec {
  name = "unstable.io.http";
  buildInputs = [ clojure ];
}
