#ifndef CPU_CTRL_H
#define CPU_CTRL_H
//-----------------------------------------------------------------------------
class CpuCtrl
{
    protected:
        virtual ~CpuCtrl() {};
    //
    public:
        typedef struct
        {
            MINT bigCore;
            MINT bigFreq;
            MINT littleCore;
            MINT littleFreq;
            MINT timeout;
        }CPU_CTRL_INFO;
        //
        static CpuCtrl* createInstance(void);
        virtual MVOID   destroyInstance(void) = 0;
        virtual MBOOL   init(void) = 0;
        virtual MBOOL   uninit(void) = 0;
        virtual MBOOL   enable(CPU_CTRL_INFO &cpuInfo) = 0;
        virtual MBOOL   disable(void) = 0;
};
//-----------------------------------------------------------------------------
#endif

