extern "C" {
    void _ZN7android6Parcel13writeString16EPKDsm(char16_t const*, unsigned long);

    void _ZN7android6Parcel13writeString16EPKtm(unsigned short const* str, unsigned long len){
        _ZN7android6Parcel13writeString16EPKDsm((char16_t const*)str, len);
    }

    void _ZN7android6Parcel13writeString16EPKtj(const char16_t* str, unsigned int len) {
        _ZN7android6Parcel13writeString16EPKDsm(str, len);
    }

}
