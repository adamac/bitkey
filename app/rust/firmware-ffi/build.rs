fn main() {
    println!("cargo:rerun-if-changed=uniffi.toml");

    let udl = "src/firmware.udl";
    uniffi::generate_scaffolding(udl).unwrap();
}
